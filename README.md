# nspire-communication

Ti-Nspire SMS / Messenger / Infrared addon programms.

This is for using the Ti-Nspire([link](https://education.ti.com/fr/products/calculators/graphing-calculators/ti-nspire-cx)) to send SMS / message via messenger. 
Codewalrus thread: [link](https://codewalr.us/index.php?topic=2299)


## Build yourself from source

StackEdit stores your files in your browser, which means all your files are automatically saved locally and are accessible **offline!**

### Android app
To be documented

### Messenger server

After you have clone the repo:

    cd messenger-server/
    ./build.sh

### Ti-Nspire app

After you have clone the repo:

    cd nspire-client/
    cd uart_<app name>/
    make
   Output file for the ti is `helloworld-cpp.tns`

   
## ToDo's
### Infrared
 - Everything related to ir (/nspire-client/uart_ir) + app
 - Get Ir codes of videos projectors /tv

### App
-	Correct bugs and **rare** random crashes :)

### Other
-	Add stl files for shield

