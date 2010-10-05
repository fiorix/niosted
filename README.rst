=======
Niosted
=======
:Info: See `github <http://github.com/fiorix/niosted>`_ for the latest source.
:Author: Alexandre Fiori <fiorix@gmail.com>


About
=====

This is an experimental `Reactor <http://en.wikipedia.org/wiki/Reactor_pattern>`_ for `Java NIO <http://en.wikipedia.org/wiki/New_I/O>`_, based on the `Python Twisted <http://twistedmatrix.com/trac/>`_ API.

I recently ported `Cyclone <http://github.com/fiorix/cyclone>`_ from Twisted to `Netty <http://jboss.org/netty>`_, using `Jython <http://www.jython.org/>`_. Then I decided to write something similar to Twisted using plain Java NIO, in order to test with Cyclone and compare the results.

Netty is a very nice project, but `look at these <http://blog.urbanairship.com/blog/tag/java/>`_ numbers. I ended up with clean and simple code, and very impressive results.


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
    # ab -n 100000 -c 25 http://192.168.0.7:8888/
    This is ApacheBench, Version 2.3 <$Revision: 655654 $>
    Copyright 1996 Adam Twiss, Zeus Technology Ltd, http://www.zeustech.net/
    Licensed to The Apache Software Foundation, http://www.apache.org/

    Benchmarking 192.168.0.7 (be patient)
    Completed 10000 requests
    Completed 20000 requests
    Completed 30000 requests
    Completed 40000 requests
    Completed 50000 requests
    Completed 60000 requests
    Completed 70000 requests
    Completed 80000 requests
    Completed 90000 requests
    Completed 100000 requests
    Finished 100000 requests


    Server Software:        CycloneServer/0.4
    Server Hostname:        192.168.0.7
    Server Port:            8888

    Document Path:          /
    Document Length:        14 bytes

    Concurrency Level:      25
    Time taken for tests:   19.406 seconds
    Complete requests:      100000
    Failed requests:        0
    Write errors:           0
    Total transferred:      17000170 bytes
    HTML transferred:       1400014 bytes
    Requests per second:    5152.93 [#/sec] (mean)
    Time per request:       4.852 [ms] (mean)
    Time per request:       0.194 [ms] (mean, across all concurrent requests)
    Transfer rate:          855.48 [Kbytes/sec] received

    Connection Times (ms)
                  min  mean[+/-sd] median   max
    Connect:        0    1  27.6      0    1007
    Processing:     0    4   3.1      4     364
    Waiting:        0    4   3.1      4     364
    Total:          1    5  27.7      5    1008

    Percentage of the requests served within a certain time (ms)
      50%      5
      66%      5
      75%      5
      80%      5
      90%      5
      95%      5
      98%      6
      99%      6
     100%   1008 (longest request)


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
    # ab -n 100000 -c 25 http://192.168.0.7:8888/
    This is ApacheBench, Version 2.3 <$Revision: 655654 $>
    Copyright 1996 Adam Twiss, Zeus Technology Ltd, http://www.zeustech.net/
    Licensed to The Apache Software Foundation, http://www.apache.org/

    Benchmarking 192.168.0.7 (be patient)
    Completed 10000 requests
    Completed 20000 requests
    Completed 30000 requests
    Completed 40000 requests
    Completed 50000 requests
    Completed 60000 requests
    Completed 70000 requests
    Completed 80000 requests
    Completed 90000 requests
    Completed 100000 requests
    Finished 100000 requests


    Server Software:        CycloneServer/0.4
    Server Hostname:        192.168.0.7
    Server Port:            8888

    Document Path:          /
    Document Length:        14 bytes

    Concurrency Level:      25
    Time taken for tests:   35.778 seconds
    Complete requests:      100000
    Failed requests:        0
    Write errors:           0
    Total transferred:      17000000 bytes
    HTML transferred:       1400000 bytes
    Requests per second:    2794.99 [#/sec] (mean)
    Time per request:       8.945 [ms] (mean)
    Time per request:       0.358 [ms] (mean, across all concurrent requests)
    Transfer rate:          464.01 [Kbytes/sec] received

    Connection Times (ms)
                  min  mean[+/-sd] median   max
    Connect:        0    0   9.1      0     928
    Processing:     1    9   1.4      8     311
    Waiting:        1    9   1.4      8     311
    Total:          2    9   9.2      9     936

    Percentage of the requests served within a certain time (ms)
      50%      9
      66%      9
      75%      9
      80%      9
      90%      9
      95%     12
      98%     13
      99%     13
     100%    936 (longest request)


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
