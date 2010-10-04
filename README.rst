=======
Niosted
=======
:Info: See `github <http://github.com/fiorix/niosted>`_ for the latest source.
:Author: Alexandre Fiori <fiorix@gmail.com>


About
=====

This is an experimental `Reactor <http://en.wikipedia.org/wiki/Reactor_pattern>`_ for `Java NIO <http://en.wikipedia.org/wiki/New_I/O>`_, based on the `Python Twisted <http://twistedmatrix.com/trac/>`_ API.

I recently ported `Cyclone <http://github.com/fiorix/cyclone>`_ from Twisted to `Netty <http://jboss.org/netty>`_, using `Jython <http://www.jython.org/>`_. Then I decided to write something similar to Twisted using plain Java NIO, in order to test with Cyclone and compare the results.

Netty is a very nice project, but `look at these <http://blog.urbanairship.com/blog/tag/java/>`_ numbers.

I ended up with clean and simple code, and very impressive results.


Building
--------

I built this code using JDK 1.6.
Just type ``ant`` and it should work. (Can't use Eclipse, I'm a ``vim`` person)

Caveats
-------

If you print information to the console, performance degrades by 40%.

The current version of Cyclone shipped with Niosted is very slow comparing to the following benchmarks. This is because the Python code has to be updated to meet Jython's requirements. It's still a work in progress.


Benchmarks
==========

I won't publish the results of this code running on my Core i5 Macbook Pro. Would seem fake.
Instead, I created the following virtual machine on Xen.

Processor::

    # grep "model name" /proc/cpuinfo 
    model name      : Intel(R) Pentium(R) Dual  CPU  E2180  @ 2.00GHz


Memory (after the tests)::

    # free -m
                 total       used       free     shared    buffers     cached
    Mem:           512        488         23          0         26        286
    -/+ buffers/cache:        175        336
    Swap:           63          8         55


Fake HTTP server (java version)
-------------------------------

Build::

    # javac -cp niosted.jar FakeHTTP.java


Run::

    # ulimit -n 12000
    # java -cp niosted.jar:. FakeHTTP


Test::

    # ulimit -n 12000
    # ab -n 10000 -c 25 http://192.168.0.7:8888/
    This is ApacheBench, Version 2.3 <$Revision: 655654 $>
    Copyright 1996 Adam Twiss, Zeus Technology Ltd, http://www.zeustech.net/
    Licensed to The Apache Software Foundation, http://www.apache.org/

    Benchmarking 192.168.0.7 (be patient)
    Completed 1000 requests
    Completed 2000 requests
    Completed 3000 requests
    Completed 4000 requests
    Completed 5000 requests
    Completed 6000 requests
    Completed 7000 requests
    Completed 8000 requests
    Completed 9000 requests
    Completed 10000 requests
    Finished 10000 requests

    Server Software:        CycloneServer/0.4
    Server Hostname:        192.168.0.7
    Server Port:            8888

    Document Path:          /
    Document Length:        14 bytes

    Concurrency Level:      25
    Time taken for tests:   2.085 seconds
    Complete requests:      10000
    Failed requests:        0
    Write errors:           0
    Total transferred:      1700000 bytes
    HTML transferred:       140000 bytes
    Requests per second:    4796.87 [#/sec] (mean)
    Time per request:       5.212 [ms] (mean)
    Time per request:       0.208 [ms] (mean, across all concurrent requests)
    Transfer rate:          796.36 [Kbytes/sec] received

    Connection Times (ms)
                  min  mean[+/-sd] median   max
    Connect:        0    0   0.1      0       3
    Processing:     1    5   3.0      5      48
    Waiting:        1    5   3.0      5      48
    Total:          2    5   3.0      5      48

    Percentage of the requests served within a certain time (ms)
      50%      5
      66%      5
      75%      5
      80%      5
      90%      5
      95%      5
      98%      6
      99%      9
     100%     48 (longest request)


Fake HTTP server (jython version)
---------------------------------

Using Jython 2.5.2b2::

    # cd jython2.5.2b2
    # cp jython.jar jythonlib.jar
    # zip -r jythonlib.jar Lib/


Run::

    # java -cp niosted:. -jar jythonlib.jar FakeHTTP.py


Test::

    # ulimit -n 12000
    # ab -n 10000 -c 25 http://192.168.0.7:8888/
    This is ApacheBench, Version 2.3 <$Revision: 655654 $>
    Copyright 1996 Adam Twiss, Zeus Technology Ltd, http://www.zeustech.net/
    Licensed to The Apache Software Foundation, http://www.apache.org/

    Benchmarking 192.168.0.7 (be patient)
    Completed 1000 requests
    Completed 2000 requests
    Completed 3000 requests
    Completed 4000 requests
    Completed 5000 requests
    Completed 6000 requests
    Completed 7000 requests
    Completed 8000 requests
    Completed 9000 requests
    Completed 10000 requests
    Finished 10000 requests


    Server Software:        CycloneServer/0.4
    Server Hostname:        192.168.0.7
    Server Port:            8888

    Document Path:          /
    Document Length:        14 bytes

    Concurrency Level:      25
    Time taken for tests:   3.554 seconds
    Complete requests:      10000
    Failed requests:        0
    Write errors:           0
    Total transferred:      1700000 bytes
    HTML transferred:       140000 bytes
    Requests per second:    2813.68 [#/sec] (mean)
    Time per request:       8.885 [ms] (mean)
    Time per request:       0.355 [ms] (mean, across all concurrent requests)
    Transfer rate:          467.12 [Kbytes/sec] received

    Connection Times (ms)
                  min  mean[+/-sd] median   max
    Connect:        0    0   0.1      0       4
    Processing:     1    9   3.1      8      49
    Waiting:        1    9   3.1      8      49
    Total:          3    9   3.1      9      50

    Percentage of the requests served within a certain time (ms)
      50%      9
      66%      9
      75%      9
      80%      9
      90%      9
      95%      9
      98%     11
      99%     13
     100%     50 (longest request)


Cyclone (jython version)
------------------------

Before generating this package, you need to install ``simplejson`` for Jython. It is required by Cyclone.
I couldn't manage to get ``Lib/site-packages/simplejson`` properly imported when using Jython embedded in Cyclone's JAR.

Generating cyclone.jar::
    
    # cp ˜/jython2.5.2b2/jythonlib.jar cyclone.jar
    # ln -s ~/jython2.5.2b2/Lib/site-packages/simplejson
    # zip -r cyclone.jar simplejson
    # zip -r cyclone.jar cyclone


Running a real HTTP server::

    # java -jar cyclone.jar HelloWorld.py


You may test with either ``ab`` or simply ``curl http://localhost:8888/``. It works :)

Performance tests shows up to 800 req/s on the same server. It is almost as fast as the original Cyclone with Twisted, and way faster than Web.py and Django. By the way, Tornado can handle up to 1800 req/s on this server.
