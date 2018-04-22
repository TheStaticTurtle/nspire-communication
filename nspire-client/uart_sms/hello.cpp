#include <os.h>
#include <nspireio/uart.hpp>
#include <nspireio/console.hpp>

int key_already_pressed_ctrl = 0; // 
int key_already_pressed_contact = 0;
int key_already_pressed_help = 0;
char* contact = "%%|NUMS|6|Name1|Name2|Name3|Name4|Name6|Name7";
int hasSelectedContact = 0;


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


int main(void)
{
	//Write the program title
	assert_ndless_rev(874);
	nio_console csl;
	nio_init(&csl,NIO_MAX_COLS,NIO_MAX_ROWS,0,0,NIO_COLOR_WHITE,NIO_COLOR_BLACK,TRUE);
	nio_set_default(&csl);
	nio_color(&csl,NIO_COLOR_BLACK,NIO_COLOR_WHITE);
	nio_printf("               Nspire UART Chat by Samy.             ");
	nio_printf("         Compiled the %s At %s        ",__DATE__,__TIME__);
	nio_color(&csl,NIO_COLOR_BLACK,NIO_COLOR_LIGHTRED);
	nio_printf("      Press ESC key to exit and H to get an help.     ");
	nio_printf("                                                    ");
	nio_color(&csl,NIO_COLOR_WHITE,NIO_COLOR_BLACK);
	nio_puts("");

	//Send that we are online
	uart_printf("#$#CALC:ENTERING:SMS\r\n");

	//Main loop
	while(!isKeyPressed(KEY_NSPIRE_ESC)){
		//Part where we send messages from the calc
		if(isKeyPressed(KEY_NSPIRE_CTRL) && !key_already_pressed_ctrl && !key_already_pressed_help && !key_already_pressed_contact){
			//Test if the user has already selected an contact
			if(hasSelectedContact) {
				// Prompting the user
				nio_printf("<< ");
				char output[90] = {0};
				nio_getsn(output,90);

				//Sending the data
				uart_printf("$#$SMS:");
				int num = numberOfCharsInArray(output);
				for(char* it = output; *it; ++it) {
					uart_printf("%c",*it);
					sleep(100);
				}
				uart_printf("\r\n");
			} else {
				// If not do nothing
				nio_printf("You must select an contact !\r\n");
			}
			key_already_pressed_ctrl = 1;
		}
		if(isKeyPressed(KEY_NSPIRE_C) && !key_already_pressed_help && !key_already_pressed_contact && !key_already_pressed_ctrl){
			// Contact select part
			nio_clear(&csl);
			//Title of the contact selection
			nio_color(&csl,NIO_COLOR_BLACK,NIO_COLOR_WHITE);
			nio_printf("                        Contact                      ");
			nio_color(&csl,NIO_COLOR_WHITE,NIO_COLOR_BLACK);

			// Prompting the user to search for someone
			nio_printf("Name search: ");
			char output[90] = {0};
			nio_getsn(output,90);

			//Sending search request
			uart_printf("$#$UPDATENUM:%s\r\n",output);

			//Black waiting screen until data come backs
			nio_color(&csl,NIO_COLOR_BLACK,NIO_COLOR_WHITE);
			nio_clear(&csl);
			while(!uart_ready() && releaseFunc()) { }
			nio_color(&csl,NIO_COLOR_WHITE,NIO_COLOR_BLACK);
			nio_clear(&csl);

			//Now that ther is data get it
			char input[1024] = {0};
			uart_getsn_mdf(input,1024);
			contact = input;

			//Menu selection title
			nio_clear(&csl);
			nio_color(&csl,NIO_COLOR_BLACK,NIO_COLOR_WHITE);
			nio_printf("                        Contact                      ");
			nio_color(&csl,NIO_COLOR_WHITE,NIO_COLOR_BLACK);

			// Print the list of contacts
			nio_puts("Select your contact:");

			//	-Cutting the string in multiples part and printing them
		        char* namestable[1024];
		        char* s;
		        int i=0;
		        s = strtok(contact ,"|");
       			nio_printf("  -1) None. \r\n");
        		while (s != NULL) {
	        		if(i>1) {
					namestable[i] = s;
				}
	        		if(i>2) {
	        			nio_printf("  %i) %s. \r\n",i-3,s);
	                	}
               			s = strtok (NULL, "|");
                		i++;
        		}

			//Prompt the user for witch contact he wants
        		nio_printf("Change contact to (n)?");
        		char selected[10] = {0};
        		nio_getsn(selected,10);

			//Converting the str to int and prossesing it
        		int ii = atoi(selected);
			ii=ii+3;
        		if(ii >= 3 && namestable[ii] != "None") {
        		        nio_printf("Changing send contact to %s. \r\n",namestable[ii]);
				uart_printf("$#$CHANGENUM:%s\r\n",namestable[ii]);
				hasSelectedContact = 1;
				nio_clear(&csl);

				nio_color(&csl,NIO_COLOR_WHITE,NIO_COLOR_GREEN);
				nio_printf(" Info: Now chating with: %s \r\n", namestable[ii]);
				nio_puts("");
        		} else {
				hasSelectedContact = 0;
				nio_clear(&csl);
				nio_color(&csl,NIO_COLOR_BLACK,NIO_COLOR_LIGHTRED);
				nio_printf("                                                     ");
				nio_printf("  Error: Error while selecting contact please retry. ");
				nio_printf("          If the error persist contact samy :)       ");
				nio_printf("           (Normal if you selected -1 (None))        ");
				nio_printf("                                                     ");
				nio_color(&csl,NIO_COLOR_WHITE,NIO_COLOR_BLACK);
				nio_puts("");
        		}
			nio_color(&csl,NIO_COLOR_WHITE,NIO_COLOR_BLACK);
        		// END Contact select part
			key_already_pressed_contact = 1;
		}

		//Help message
		if(isKeyPressed(KEY_NSPIRE_H) && !key_already_pressed_ctrl && !key_already_pressed_help && !key_already_pressed_contact){
			nio_clear(&csl);
                        nio_color(&csl,NIO_COLOR_BLACK,NIO_COLOR_WHITE);
                        nio_printf("                                                     ");
                        nio_printf("                      Help :                         ");
                        nio_printf("                                                     ");
                        nio_color(&csl,NIO_COLOR_WHITE,NIO_COLOR_BLACK);
			nio_puts("Keymap:");
			nio_puts("\t ESC   -> Quit the program:");
			nio_puts("\t CTRL  -> Send an message");
			nio_puts("\t R     -> Press if the programm is stuck");
			nio_puts("\t E     -> Clear the screen");
			nio_puts("\t C     -> Select an contact");
			nio_puts("");
			nio_puts("Press any key to exit");
			wait_key_pressed();
			nio_clear(&csl);
		}
		
		//ir
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

		//Reset keyvent if no key are pressed
		if(!any_key_pressed()) {
			key_already_pressed_ctrl = 0;
			key_already_pressed_contact = 0;
			key_already_pressed_help = 0;
		}


		// If there is Incoming data just printing it out
		if(uart_ready()) {
			char input[1024] = {0};
			uart_getsn_mdf(input,1024);
			nio_puts(input);
		}
	}

	//Send that we are offline
	uart_printf("#$#CALC:LEAVING:SMS\r\n");

	//Exiting
	nio_puts("Offline message sended. Exiting ...");
	nio_free(&csl);
	return 0;
}
