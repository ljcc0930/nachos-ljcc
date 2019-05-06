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
#include "atoi.c"

int main(int argc, char **argv)
{
    
    int n;
    int m;

    if (argc == 1) {
        n = 0;
        m = 0;
    }
    else {
        n = atoi(argv[1]);
        m = atoi(argv[2]);
    }

    char f1[20];
    char f2[20];
    char buf[20];

    sprintf(f1, "fb_temp_a_%d", m);
    sprintf(f2, "fb_temp_b_%d", m);

    unsigned a = 0;
    unsigned b = 1;
    unsigned c;

    int fd1 = creat(f1);
    int fd2 = creat(f2);

    sprintf(buf, "%d", a);
    write(fd1, buf, 20);
    sprintf(buf, "%d", b);
    write(fd2, buf, 20);
    close(fd1);
    close(fd2);

    int i;    
    for (i = 0; i < 100; ++i) {
        fd1 = open(f1);
        fd2 = open(f2);
        read(fd2, buf, 20);
        b = atoi(buf);
        read(fd1, buf, 20);
        a = atoi(buf);
        c = a + b;
        
        close(fd1);
        close(fd2);
        fd1 = open(f1);
        sprintf(buf, "%d", b);
        write(fd1, buf, 20);
        fd2 = open(f2);
        sprintf(buf, "%d", c);
        write(fd2, buf, 20);
        close(fd1);
        close(fd2);
    }       

    unlink(f1);
    unlink(f2);

    assertx(c == (int) -1869596475, -1, "wrong 3000 result.");
    printf("--- PASS fibonacci %d\n", m);
    exit(n);
}
