
Method of key generation:

The specs do not state the requirements for the format of the shortened URL. I have based
my implementation of off TinyURL.
TinyURL seems to generate 8 character URL keys consisting of lowercase letters and numbers,
which results in 3.24 ^ 32 possible values.


