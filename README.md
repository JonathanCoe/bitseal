Bitseal
=======

[![tip for next commit](http://prime4commit.com/projects/103.svg)](http://prime4commit.com/projects/103) [![tip for next commit](https://tip4commit.com/projects/1001.svg)](https://tip4commit.com/github/JonathanCoe/bitseal)

Bitseal is a Bitmessage client for Android, currently in Beta. 

<img src="https://i.imgur.com/45OuinZ.png" align="left" height="620" width="349" hspace="5" vspace="20">
<img src="https://i.imgur.com/R2xgeDW.png" align="left" height="620" width="349" hspace="5" vspace="20">


## Screenshots:	https://imgur.com/a/utC00


## Working features:
+ Sending messages
+ Receiving messages
+ QR codes for addresses
+ Address Book
+ Import addresses
+ Export addresses
+ Choose which servers to use (including your own)


## Roadmap for development (subject to change):
+ Update for Bitmessage Protocol Version 3
+ Lite client message retrieval using prefix filters (credit to Peter Todd for suggesting this)
+ Local encryption of the database, using SQLCipher
+ SSL for connections between clients and servers
+ POW implemented in C or C++ via the Android NDK
+ Support for broadcasts
+ Refresh the UI
+ "Panic wipe" secure deletion of all local data


## Notes:
+ The beta release should be used for testing purposes only. A first production release is planned once lite client message retrieval is implemented.
+ Bitseal is free, open source software, released under the Gnu General Public License Version 3. 
+ Some parts of Bitseal include, are based on, or are reliant upon software written by others, including [Jonathan Warren](https://github.com/Atheros1), [Sebastian Schmidt](https://github.com/ISibboI), [Tim Roes](https://github.com/timroes), [Roberto Tyley](https://github.com/rtyley), the [bitcoinj](https://github.com/bitcoinj/bitcoinj) developers, and the [Bouncy Castle](https://www.bouncycastle.org/java.html) developers. This is noted in the source code where applicable.
+ I have set up a few default servers which can be used for testing purposes. Anyone is free to set up and use their own. The server application is simply a copy of PyBitmessage with a modified API. 
+ The development of Bitseal has been a large project, taking many months of work. I'm doing it on a purely non-profit basis. Bitcoin donations are very gratefully received: **1L7amdWrPv4R4f1vLdanr2xU71TPs3wUEC**

LICENSE
---------------
Copyright (C) 2014 - 2014  bitseal

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>