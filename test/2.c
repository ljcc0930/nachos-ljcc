#include "syscall.h"
#include "strcmp.c"
#include "strcpy.c"
#include "assert.c"

int main()
{
    int i,n=10,pid,status;
	char buf[20];
	char *argv[2];
	for (i=1;i<=n;i++)
		{
		sprintf(buf, "%d", i);
		argv[0]="1.coff";
		argv[1]=buf;
		pid = exec("1.coff", 2, argv);
		join(pid,&status);
		printf("new pid:%d %d\n",status,pid);
		}
	pid = exec("asd.coff", 1, argv);
	printf("no process:%d\n",pid);
	pid = join(100,&status);
	printf("join un-child:%d\n",pid);
	exit(0);
}
