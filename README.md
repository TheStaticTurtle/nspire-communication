
# nspire-communication
## Presentation
Ti-Nspire SMS / Messenger / Infrared addon programms.

This is for using the Ti-Nspire([link](https://education.ti.com/fr/products/calculators/graphing-calculators/ti-nspire-cx)) to send SMS / message via messenger. 
Codewalrus thread: [link](https://codewalr.us/index.php?topic=2299)

First some pictures:
![](https://github.com/TurtleForGaming/nspire-communication/raw/master/img/IMG_20180422_230404.jpg)
![](https://github.com/TurtleForGaming/nspire-communication/raw/master/img/IMG_20180422_230429.jpg)
(Video is comming)

## Usage
	
 - Start the messenger server (If you want ot use it)
 - Start the android app
 - Connect to the bluetooth module in the shield
 - Open one of the ti  programms
 - Start chating

## How to build it

### Configuration
 - You will need to put your bluetooth module into at mode and cofigure the baudrate to be 115200bps

### Build the source code
#### Android app
To be documented

#### Messenger server

After you have clone the repo:

    cd messenger-server/
    ./build.sh

#### Ti-Nspire app

After you have clone the repo:

    cd nspire-client/
    cd uart_<app name>/
    make
   Output file for the ti is `helloworld-cpp.tns`

### Build the shield
Just melt the connector in place and pass some wire here:
![](https://raw.githubusercontent.com/TurtleForGaming/nspire-communication/master/img/IMG_20180423_121145.jpg)
 
| n° | Usage |
|--|--|
|Pin 1| NC |
|Pin 2| VCC |
|Pin 3| TX |
|Pin 4| RX |
|Pin 5 | GND | 

### Electronics
Schematic comming soon

### ToDo's
#### Infrared
 - Everything related to ir (/nspire-client/uart_ir) + app
 - Get Ir codes of videos projectors /tv

#### App
-	Correct bugs and **rare** random crashes :)

