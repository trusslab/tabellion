#ifndef _PIMAGE_H_
#define _PIMAGE_H_

typedef struct {
	pid_t pid;
	int infd,
	outfd,
	errfd;
} processimage;

static void
mkprocess(const char *cmd, processimage *ret, const char in, const char out, const char err) {
	int infd[2], outfd[2], errfd[2];
	pid_t child;
	
	if(in)
		if(pipe(infd))
			return;
	if(out)
		if(pipe(outfd))
			return;
	if(err)
		if(pipe(errfd))
			return;
	
	switch((child = fork())) {
		case -1:
			return;
			
		case 0:
			if(in) {
				close(STDIN_FILENO);
				dup(infd[0]);
				close(infd[1]);
			}
			if(out) {
				close(STDOUT_FILENO);
				dup(outfd[1]);
				close(outfd[0]);
			}
			if(err) {
				close(STDERR_FILENO);
				dup(errfd[1]);
				close(errfd[0]);
			}
			execlp(cmd, cmd, NULL);
			return;
			
		default:
			ret->pid = child;
			if(in) {
				close(infd[0]);
				ret->infd = infd[1];
			}
			if(out) {
				close(outfd[1]);
				ret->outfd = outfd[0];
			}
			if(err) {
				close(errfd[1]);
				ret->errfd = errfd[0];
			}
	}
	return;
}

static void
rmprocess(processimage *pimage) {
	if(pimage->infd) {
		close(pimage->infd);
		pimage->infd = 0;
	}
	if(pimage->outfd) {
		close(pimage->outfd);
		pimage->outfd = 0;
	}
	if(pimage->errfd) {
		close(pimage->errfd);
		pimage->errfd = 0;
	}
	waitpid(pimage->pid, NULL, 0);
	pimage->pid = 0;
}

#endif
