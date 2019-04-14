
/*
Author: Daniel Yan
Program: processes.cpp
Purpose: Introduction to using Unix fork() and pipe creation.
Emulate linux command of: ps -A | grep argv[1] | wc -l
*/

#include <stdlib.h>  //exit
#include <stdio.h>   //perror
#include <unistd.h>  //fork, pipe
#include <sys/wait.h> //wait

int main(int argc, char * argv[])
{
    //Check number of arguments
    if (argc < 2)
    {
       perror("Argument values are too few");
       exit(EXIT_FAILURE); //Exit main
    }
   //Enum for 2 states possible in pipe
   enum {READ, WRITE};
   //const int READ = 0, WRITE = 1;
   //Set a pid id for the fork()
   pid_t pid;
   //Make pipe arrays for necessary pipe states
   int pipeFD1[2], pipeFD2[2];
   //Status to wait fro all childs to finish
   int childStatus;
    //Create pipes and check read state
   if (pipe(pipeFD1) < 0 || pipe(pipeFD2) < 0)
   {
      perror("Pipe error creation");
      exit(EXIT_FAILURE);
   }

    //Set pid id to fork(), check if fail state is had in creation
    //Child process for fork
   if ((pid = fork()) < 0)
   {
      perror("Error during first fork");
      exit(EXIT_FAILURE);
   }
   //Run child process for the fork
   else if (pid == 0)
   {
     //Check if error is had during grandchild fork
     if ((pid = fork()) < 0)
     {
        perror("Error during second fork");
        exit(EXIT_FAILURE);
     }
     //Run grandchild process for the fork
     else if (pid ==  0)
     {
       //Check if error is had during great-grandchild fork
       if ((pid = fork()) < 0)
       {
          perror("Error during third fork");
          exit(EXIT_FAILURE);
       }
       //Run great-grandchild process for the fork
       else if (pid == 0)
       {
         //Great-grandchild. Reads from grandchild only.
         //Close unnecessary pipe states
         close(pipeFD1[WRITE]);
         close(pipeFD1[READ]);
         close(pipeFD2[WRITE]);
         dup2(pipeFD2[READ], READ);    //Read input from grandchild's write pipe
         execlp("wc", "wc", "-l", NULL); // Executes wc -l command
       }
       //Grandchild. Write to child and reads from great-grandchild
       //Close unnecessary pipe states
       close(pipeFD1[WRITE]);
       close(pipeFD2[READ]);
       dup2(pipeFD1[READ], READ);   //Read input from child's write pipe
       dup2(pipeFD2[WRITE],WRITE);   //stdout (1) is not great-greatchild's read pipe
       execlp("grep", "grep", argv[1], NULL); // Executes grep argv[1] command
     }
     //Child. Only writes input to grandchild
     //Close unnecessary pipe states
       close(pipeFD1[READ]);
       close(pipeFD2[READ]);
       close(pipeFD2[WRITE]);
       dup2(pipeFD1[WRITE], WRITE);    //stdout (1) is now granchild's read pipe
       execlp("ps", "ps", "-A", NULL); // Executes ps -A command
    }
   //Parent process
   else
   {
     //Wait for all child processes to finish
      wait(&childStatus);
      exit(EXIT_SUCCESS); //Finished successfully
   }
}
