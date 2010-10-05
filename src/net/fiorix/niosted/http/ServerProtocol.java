/*
# Copyright 2010 Alexandre Fiori
# niosted
#
# Licensed under the Apache License, Version 2.0 (the "License"); you may
# not use this file except in compliance with the License. You may obtain
# a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
# License for the specific language governing permissions and limitations
# under the License.
*/

package net.fiorix.niosted.http;

import java.util.concurrent.ConcurrentHashMap;
import net.fiorix.niosted.protocols.LineReceiver;

public class ServerProtocol extends LineReceiver
{
    public String delimiter = "\r\n";

    private String[] request_data = null;
    private ConcurrentHashMap headers = null;
    private StringBuffer content_buffer = null;
    private int content_length = 0;

    public void prepare(String method, String uri, String version, ConcurrentHashMap headers)
    {
    }

    public void ready(String request_body)
    {
    }

    public void lineReceived(String line)
    {
        if(this.headers == null) {
            if(line == null) {
                log("Malformed HTTP request: Empty request");
                this.transport.loseConnection();
                return;
            }

            String[] start_line = line.split(" ");
            if(start_line.length != 3) {
                log("Malformed HTTP request: " + line);
                this.transport.loseConnection();
                return;
            }

            if(start_line[2].startsWith("HTTP/")) {
                this.request_data = start_line;
            } else {
                log("Malformed HTTP version in HTTP Request-Line: " + line);
                this.transport.loseConnection();
                return;
            }

            this.headers = new ConcurrentHashMap();

        } else if(line != null) {
            String[] temp = line.split(":", 2);
            try {
                this.headers.put(temp[0], 
                    temp[1].charAt(0) == ' ' ? temp[1].substring(1, temp[1].length()) : temp[1]);
            } catch(Exception ex) {
                log("Malformed HTTP header: " + line);
                this.transport.loseConnection();
                return;
            }

        } else {
            int content_length = 0;

            if(this.headers.containsKey("Content-Length")) {
                String length = (String) this.headers.get("Content-Length");
                try {
                    content_length = Integer.parseInt(length);
                } catch(Exception ex) {
                    log("Malformed HTTP headers contain invalid Content-Length: " + length);
                    this.transport.loseConnection();
                    return;
                }
            }

            this.prepare(this.request_data[0], this.request_data[1], this.request_data[2], this.headers);
            this.request_data = null;
            this.headers = null;

            if(content_length >= 1) {
                String expect = (String) this.headers.get("Expect");
                if(expect != null) {
                    if(expect.contentEquals("100-continue"))
                        this.transport.write("HTTP/1.1 100 (Continue)\r\n\r\n");
                }
                this.content_length = content_length;
                this.content_buffer = new StringBuffer(content_length);
                this.setRawMode();
                return;
            }

            this.ready(null);
        }
    }

    public void rawDataReceived(String data)
    {
        String rest = "";
        String chunk = null;
        int data_length = data.length();

        if(this.content_length >= 1) {
            if(this.content_length < data_length) {
                chunk = data.substring(0, this.content_length);
                rest = data.substring(this.content_length, data_length);
            } else {
                chunk = data.substring(0, data_length);
            }

            this.content_length -= data_length;
        }

        this.content_buffer.append(chunk);
        if(this.content_length == 0) {
            this.ready(this.content_buffer.toString());
            this.content_buffer = null;
            this.content_length = 0;
            if(rest.length() >= 1)
                this.setLineMode(rest);
        }
    }

    private void log(String text)
    {
        java.lang.System.out.println(text);
    }
}
