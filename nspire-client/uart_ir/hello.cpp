#include <os.h>
#include <nspireio/uart.hpp>
#include <nspireio/console.hpp>
#include <iostream>
#include <fstream>
#include <string>
#include <libndls.h>

using namespace std;

// Function added with && on every loop to prevent the programm of getting stuck
int releaseFunc() {
	if(isKeyPressed(KEY_NSPIRE_R) || isKeyPressed(KEY_NSPIRE_ESC)) {
		return 0;
	} else {
		return 1;
	}
}

// Modidification of the uart_getsn() function to remove the echo. And the 32 FIFO Limit
char* uart_getsn_mdf(char* str, int num) {
        int i = 0;
        int max = num-1;
        while(1 && releaseFunc()) {
                while(uart_ready() && releaseFunc()) {
                        if(i == max) {
                                str[i] = 0;
                                return str;
                        }
                        char c = uart_getchar();
                        str[i] = c;
                        if(c == '\b'){  i -= 2; }
                        else if(c == '\r')      { str[i] =0; return str; }
                        i++;

                }
        }
}

// Return the number of char in a car array
int numberOfCharsInArray(char* array) {
  return strlen(array);
}

bool startsWith(const char *pre, const char *str)
{
    size_t lenpre = strlen(pre),
           lenstr = strlen(str);
    return lenstr < lenpre ? false : strncmp(pre, str, lenpre) == 0;
}

int main(void)
{
	//Write the program title
	assert_ndless_rev(874);
	nio_console csl;
	nio_init(&csl,NIO_MAX_COLS,NIO_MAX_ROWS,0,0,NIO_COLOR_WHITE,NIO_COLOR_BLACK,TRUE);
	nio_set_default(&csl);
	nio_color(&csl,NIO_COLOR_BLACK,NIO_COLOR_WHITE);
	nio_printf("                Nspire UART IR by Samy.              ");
	nio_printf("         Compiled the %s At %s        ",__DATE__,__TIME__);
	nio_color(&csl,NIO_COLOR_BLACK,NIO_COLOR_LIGHTRED);
	nio_printf("      Press ESC key to exit and H to get an help.     ");
	nio_printf("                                                    ");
	nio_color(&csl,NIO_COLOR_WHITE,NIO_COLOR_BLACK);
	nio_puts("");

	//Send that we are online
	uart_printf("#$#CALC:ENTERING:IR\r\n");

	//Main loop
	while(!isKeyPressed(KEY_NSPIRE_ESC)){
		/*ir
		if(isKeyPressed(KEY_NSPIRE_I)){
			uart_printf("$#$IR:20DF10EF\n\r");
			nio_printf("$#$IR:20DF10EF\n");
			sleep(10);
		}

		//Clear the screen
		if(isKeyPressed(KEY_NSPIRE_E)){
			sleep(100);
			nio_clear(&csl);
		}
		*/
		// If there is Incoming data just printing it out
		if(uart_ready()) {
			char input[2048] = {0};
			uart_getsn_mdf(input,2048);
			ofstream myfile;
			myfile.open ("/documents/example.txt.tns");
			myfile << "Writing this to a file.\n";
			myfile.close();
			nio_puts(input);
			//if(true) {
				//nio_puts("Writing to file");
				//std::ofstream o("\DOCUMENTS\TRANSFER\file.txt.tns");
				//o << input << "\n" << std::endl;

				//FILE * fp;
				//fp = fopen ("\DOCUMENTS\TRANSFER\file.txt.tns", "w+");
				//fprintf(fp,"%s", input);
				//fclose(fp);
			//}
		}
	}

	//Send that we are offline
	uart_printf("#$#CALC:LEAVING:SMS\r\n");

	//Exiting
	nio_puts("Offline message sended. Exiting ...");
	nio_free(&csl);
	return 0;
}
