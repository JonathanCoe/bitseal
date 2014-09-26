Bitseal
=======

Bitseal is a Bitmessage client for Android, currently in Beta. 

<a href="Inbox"><img src="https://i.imgur.com/45OuinZ.png" align="left" height="620" width="349" hspace="5" vspace="20"></a>
<a href="Sent"><img src="https://i.imgur.com/R2xgeDW.png" align="left" height="620" width="349" hspace="5" vspace="20" ></a>

<br><br>  


##Screenshots:	https://imgur.com/a/utC00


##Working features:
- Sending messages

- Receiving messages

- QR codes for addresses

- Address Book

- Import addresses

- Export addresses

- Choose which servers to use (including your own)


##Roadmap for development (subject to change):
- Update for Bitmessage Protocol Version 3

- Lite client message retrieval using prefix filters (credit to Peter Todd for suggesting this)

- Local encryption of the database, using SQLCipher

- SSL for connections between clients and servers

- POW implemented in C or C++ via the Android NDK

- Support for broadcasts

- Refresh the UI

- "Panic wipe" secure deletion of all local data


##Notes:

- The beta release should be used for testing purposes only. A first production release is planned once lite client message retrieval is implemented.

- Bitseal is free, open source software, released under the Gnu General Public License Version 3. 

- Some parts of Bitseal are based or reliant upon software written by others, including <a href="Jonathan Warren">https://github.com/Atheros1</a>, <a href="Sebastian Schmidt">https://github.com/ISibboI</a>, <a href="Tim Roes">https://github.com/timroes</a>, <a href="Roberto Tyley">https://github.com/rtyley</a>, the <a href="bitcoinj">https://github.com/bitcoinj/bitcoinj</a> developers, and the <a href="Bouncy Castle">https://www.bouncycastle.org/java.html</a> developers. This is noted in the source code where applicable.

- I have set up a few default servers which can be used for testing purposes. Anyone is free to set up and use their own. The server application is simply a copy of PyBitmessage with a modified API. 

- The development of Bitseal has been a large project, taking many months of work. I'm doing it on a purely non-profit basis. Bitcoin donations are very gratefully received: **1L7amdWrPv4R4f1vLdanr2xU71TPs3wUEC**

