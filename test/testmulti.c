/* test_multiprogramming.c
 *	To test the most basic multiprogramming feature.
 *  Once passed, confirm the correctness of exec, load, loadSections.
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
#include "assertx.c"

#define NUM_CHILDPROCESS 2

int main()
{
    char multilized[] = "fdtest.coff";
    char filename[] = "test.txt.test";
    int n = NUM_CHILDPROCESS;
    int p[NUM_CHILDPROCESS];
    int i;

    char buf[NUM_CHILDPROCESS][10][20];
    char *argv[NUM_CHILDPROCESS][10];
    for (i = 0; i < n; ++i) {
        sprintf(buf[i][1], "%d", i);
        sprintf(buf[i][0], "%s", multilized);
        sprintf(buf[i][2], "%s", filename);
        argv[i][1] = buf[i][1];
        argv[i][0] = buf[i][0];
        argv[i][2] = buf[i][2];
        p[i] = exec(multilized, 3, argv[i]);
        
    }
    int status = 10;
    for (i = 0; i < n; ++i) {
        if (p[i] == -1)
            continue;
        join(p[i], &status); 
        printf("%d %d\n", i, status);
        assertx(status == i, -1, "wrong child status"); 
    }

    unlink(filename);

    printf("--- PASS multiprogramming\n");

    exit(0);
}
