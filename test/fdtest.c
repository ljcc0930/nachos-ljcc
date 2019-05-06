/* test_fd.c
 *	To test the correctness of management for file descriptors.
 *  Support multiprogramming.
 *  Once passed, confirm the correctness of fd part, open, creat.
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
#include "assertx.c"

int main(int argc, char **argv)
{
    int MAX_NUM_FD = 16;
    char filename[50] = "nachos_test_fd.txt.test";
    char buf[256];
    int p[MAX_NUM_FD];
    int multiprogramming = (argc > 1);

    if (multiprogramming) 
        printf("multiprogramming: %s\n", argv[1]);

    int n = 0;
    int fd;
    int fds[MAX_NUM_FD];

    int i, j;
    for (i = 0;  i < MAX_NUM_FD; ++i)
        fds[i] = -1;

    if (multiprogramming == false) {
        fd = open(filename);
        assertx(fd == -1, 1, "test_fd: nachos_test_fd.txt.test exists before creation. check unlink.");
        fds[0] = creat(filename);

        if (fds[0] == -1) {
            unlink(filename);
            assertx(0, 1, "test_fd: nachos_test_fd.txt.test create fails.");            
        }
    }
    else {
        assertx(argc >= 3, -1, "test_fd: argc < 3");
        strcpy(filename, argv[2]);
        n = atoi(argv[1]);
        fds[0] = creat(filename);
        if (fds[0] == -1) {
            unlink(filename);
            assertx(0, 1, "test_fd: nachos_test_fd.txt.test create fails.");
        }
        MAX_NUM_FD = 8;
    }

    close(0);

    for (i = 1; i <= MAX_NUM_FD - 1; ++i) {
        for (j = 0; j < i; ++j) {
            sprintf(buf, "test_fd: round %d fd[%d] fails", i, j); 

            if (fds[j] == -1) {
                unlink(filename);
                assertx(0, 1, buf);
            }
        }
        if (multiprogramming == false)
            printf("round %d fds: ", i);
        for (j = 0; j < i; ++j) {
            if (fds[j] == 1) {
                unlink(filename);
                assertx(0, 1, "test_fd: illegal use fd 1.");
            }    
            if (multiprogramming == false)
                printf("%d ", fds[j]);
        }
        if (multiprogramming == false)
            printf("\n");
        p[0] = 0;
        for (j = 1; j < i; ++j) {
            int x = rand() % (j + 1);
            p[j] = p[x];
            p[x] = j;
        }

        if (i == MAX_NUM_FD - 1)
            break;
        
        for (j = 0; j < i; ++j) {
            if (!(multiprogramming == true && j == 0)) {
                close(fds[p[j]]);
                fds[p[j]] = -1;
            }
        }

        for (j = 0; j <= i; ++j) {
            if (fds[j] == -1)
                fds[j] = open(filename);
        }
    }
    
    if (multiprogramming == false && open(filename) != -1) {
        unlink(filename);
        assertx(0, 1, "test_fd: available fds exceed.");
    }

    if (multiprogramming == false)
        unlink(filename);

    if (multiprogramming == false)
        printf("--- PASS test_fd. If first time run this again to check unlink\n");
    else
        printf("--- PASS test_fd %d\n", n);
    exit(n);
}
