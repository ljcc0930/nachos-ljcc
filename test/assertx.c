#include "stdio.h"
#include "stdlib.h"

void assertx(int cond, int v, const char *info) {
    if (cond == 1)
        return;
    printf("%s\n", info);
    exit(v);
}
