#!/usr/bin/env jython
# coding: utf-8
#
# Copyright 2010 Alexandre Fiori
# based on the original Tornado by Facebook
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

import profile

import cyclone.web
import cyclone.ioloop

class IndexHandler(cyclone.web.RequestHandler):
    def get(self):
        self.finish("Hello, world\r\n")

class Application(cyclone.web.Application):
    def __init__(self):
        handlers = [
            (r"/", IndexHandler)
        ]

        cyclone.web.Application.__init__(self, handlers)

def main():
    cyclone.ioloop.TCPServer(Application(), port=8888, interface="0.0.0.0")
    cyclone.ioloop.run()

if __name__ == "__main__":
    main()
