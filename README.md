##URL shortening service

###Design
The design doc does not state the requirements for the format of the shortened URL. I have based
my implementation of off TinyURL.
TinyURL seems to generate 8 character URL keys consisting of lowercase letters and numbers,
which results in 3.24 ^ 32 possible values.

Given the small key size, collisions will happen
if the service is heavily used. My solution is to use random numbers rather than hashes.
In the case of a key collision, I retry with a new random key. This would not be possible
with a hash.

The service only requires key-value storage, so I have chosen Redis. Redis is fast and supports atomic 
set-if-not-present operations, which are used in my implementation. 

###Running the Service
You must have Redis running locally.

Build and run the app with:
    sbt run



