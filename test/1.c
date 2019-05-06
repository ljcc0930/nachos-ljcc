#include "syscall.h"
#include "strcmp.c"
#include "strcpy.c"
#include "assert.c"
#include "atoi.c"

int main(int argc, char **argv)
{
    int n=0,m=0;
	if (argc>=1) n = atoi(argv[1]);
	printf("Process:%d\n",n); 
    double i;
	if (n%2==0) i=n/m;
	exit(n*n);
}
