/* test_multiprogramming.c
 *	To test the most basic multiprogramming feature.
 *  Once passed, confirm the correctness of exit, exec, join.
 * 	NOTE: for some reason, user programs with global data structures 
 *	sometimes haven't worked in the Nachos environment.  So be careful
 *	out there!  One option is to allocate data structures as 
 * 	automatics within a procedure, but if you do this, you have to
 *	be careful to allocate a big enough stack to hold the automatics!
 */

#include "syscall.h"
#include "strcmp.c"
#include "strcpy.c"
#include "assert.c"
#include "rand.c"

int main()
{
    char random_run[] = "t2test.coff";

    int repeatTimes = 100;
    int h = 1, t = 1;
    int sub[1010];
    int sig[1010];
    int status;

    char buf[3][20];
    char *argv[3];
    while (repeatTimes) {
        int sign = rand() % 16;

        sprintf(buf[0], "%s", random_run);
        sprintf(buf[1], "%d", sign);
        sprintf(buf[2], "%d", repeatTimes);
        argv[0] = buf[0];
        argv[1] = buf[1];
        argv[2] = buf[2];

        sub[t] = exec(random_run, 3, argv);
        if (sub[t] != -1) {
            sig[t] = sign;
            repeatTimes--;
            ++t;
        }
        else {
            join(sub[h], &status);
            assertx(status == sig[h], 1, "wrong status");
            ++h;
        }
    }
    while (h < t) {
        join(sub[h], &status);
        assertx(status == sig[h], 1, "wrong status");
        ++h;
    }    
    printf("--- PASS sequential fibonacci\n");

    exit(0);
}
