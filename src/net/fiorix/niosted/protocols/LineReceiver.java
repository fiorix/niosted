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

package net.fiorix.niosted.protocols;

public class LineReceiver extends DataReceiver
{
    public String delimiter = "\r\n";
    public int max_length = 16384;
    private boolean linemode = true;
    private String ibuffer = "";

    public void lineReceived(String line)
    {
    }

    public void rawDataReceived(String data)
    {
    }

    public void lineLengthExceeded(String exceeded)
    {
    }

    public void setLineMode()
    {
        this.linemode = true;
    }

    public void setLineMode(String extra)
    {
        this.linemode = true;
        this.dataReceived(extra);
    }

    public void setRawMode()
    {
        this.linemode = false;
    }

    public void dataReceived(byte[] data)
    {
        this.dataReceived(new String(data));
    }

    private void dataReceived(String data)
    {
        this.ibuffer += data;
        while(this.linemode) {
            if(this.transport.isClosed()) break;

            String[] temp = this.ibuffer.split(this.delimiter, 2);
            String line = temp[0];
            this.ibuffer = temp[1];

            if(line.length() > this.max_length) {
                String exceeded = line + this.ibuffer;
                this.ibuffer = "";
                this.lineLengthExceeded(exceeded);
                return;
            }

            this.lineReceived(line.contentEquals("") ? null : line);
            if(this.ibuffer.length() == 0) break;
        } 

        if(!this.linemode) {
            String chunk = this.ibuffer;
            this.ibuffer = "";
            if(chunk != null)
                this.rawDataReceived(chunk);
        }
    }

    public void sendLine(String line)
    {
        this.transport.write(line+this.delimiter);
    }
}
