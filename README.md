# WorkerSelection #

A tool to create work schedules which satisfy several constraints.

 
## Motivation ##
A friend of mine volunteered to create work schedules for a group of 
people with a special set of constraints. This turned out to be a 
quite cumbersome task and therefore I offered to write this program. 

The original version of this program was quickly developed as a 
proof of concept and only slightly modified to fix some bugs. It 
was rather prone to user errors and did not return error messages
which are understandable for non-developers. The revised version was 
developed to reduce the error possibilities and to return clearer 
error messages. The additional maven support should facilitate the 
usage of the used libraries. 

## How to use ##

The data input is given to the program via an excel file named 
"input.xlsx". The structure of the input file is shown in the example 
input and explained in a later section.

The program can be built with the command 
*mvn clean compile assembly:single* or run from an IDE. It has been 
tested on Ubuntu 16.04 and Windows 7. For a first test, simply 
rename the example input file to "input.xlsx".

## How it works ##
At first, WorkerSelection collects all data from the input file. The 
program will terminate if it fails to read potential crucial data to
avoid creating a plan based on incorrect data. Afterwards, for every
task of every event a pool of workers in created. The pool of workers 
is reduced to only the workers which can do the task. Then, a ranking 
process is used to determine the most suitable worker. In the last 
step, the result is written to a new excel file.    

## Example ##

The example file contains a fictional example of a sports club 
whose members train twice a week. Before each training session, they
like to have a small speech / presentation to learn something new and
after the training they have some snacks. Additionally, they have 
parties for special occasions. 

The purpose of WorkerSelection is to distribute the tasks as evenly 
as possible among the members who volunteer to do them while 
respecting all given constraints. The constraints include the 
different tasks preferences and time restrictions.

## The structure of the input file ##

The input file contains several sheets for the different types of 
information and constraints. The program will start to read the input 
starting in the first row until the first empty row / id cell. The 
sheet names are used to identify the different sheets. Therefore, they
cannot be renamed. However, their order does not matter. The following 
subsections will describe the content of the sheets in greater 
detail.

### Tasks ###
 
The tasks objects consist of an ID and a name. The ID has to be an
integer. A non-integer id will lead to a program termination. The 
name should be given as normal text (String). If no name is given, 
the ID will also be used as name.

### Period ###

This sheet contains the information about the start and end date of the
period in question. The two dates should be recognized by the 
spreadsheet as dates.

The second part of the sheet allows to exclude dates and events. 
WorkerSelection will not create any regular events for the dates or 
events specified in these lines. The dates can be entered as single 
dates or as "date strings" (explained later). The notes in the first
column are only for the user and not read by the program. 

### Regular events ###

Regular events are events which are repeated weekly with the following
inputs:
* ID: an integer value
* Name: normal text (String) - ID is used as backup
* Day: the weekday of the repeating event. Has to be in the system 
language.
* Time: the start time of the event. The spreadsheet should recognize
the input as time.
* Tasks: the IDs of the tasks which are required for the event. 
Separated by semicolons. Write the task IDs as many times as you need 
workers for the event (Input 101; 101; 102 -> two workers for 101 and 
one for 102).
* Counters: a single integer. Default should be 0. More details can
be found in the "Counters" subsection.
* Comment: a simple text (String) input. It is written to the output 
file.

### Special events ###

Special events are events that occur only once. The inputs are the 
same as for regular events with one exception: a specific date is given 
instead of a day.


### Workers ###
The workers are the people who will do the tasks. They have the 
following inputs:

* ID: an integer value
* Name: normal text (String) - ID is used as backup
* Possible tasks: the IDs of the tasks which the person will do. The
values have to be separated by semicolons.
* Preferred events: the IDs of the events which the person prefers to
do. This makes an assignment for these events more likely. The IDs 
have to be separated by semicolons. The input of ranges is also 
possible with the use of a minus sign / dash (Example input: 2; 3; 
6-8 means events 2, 3, 6, 7, 8).
* Excluded events: the IDs of the events which the worker is not 
willing to do. The input format is the same as for preferred events.
* Preferred dates: specific dates on which the worker would like to 
do a task. The input is in the form of "date strings" which is 
described in one of the following sections.
* Excluded dates: specific dates for which the worker will not be 
selected. The input is in the form of "date strings" which is 
described in one of the following sections.
* Works with: takes as input the ID of one other worker. Strongly 
increases the chances of getting selected if the other worker is 
already assigned to a task.
* Works without: takes as input the ID of one other worker. The 
worker will not be selected if the other one is already assigned to
a task.
* WorkCount last period: the number of assignments of the last period.
This count can be found in the previous output file.
* Last active: date of last activity from the last period. The 
spreadsheet should recognize the input as date. This information 
makes sure that workers who were active at the end of the last period
are not directly selected at the start of the new period.

### Settings ###
The settings sheet allows some fine-tuning. All possible settings 
can be found on the settings sheet of the example input. 
The order of the settings does not matter, but the key
words must not be changed. WorkerSelection scans the first 100 
lines, therefore empty lines can be used to structure the settings.


### "Date strings" ###
Date strings are used whenever a cell can contain more than one date. 
Date strings can contain three different components which should be
separated by semicolons:
* Single dates: these dates have to be formatted in the form of 
"dd.mm.yyyy". For example "23.08.2017".
* Single events: single events allow to specify an exact event at a 
specific date. They are formatted as "dd.mm.yyyy|eventID". For 
example "23.08.2017|4" which means an event with ID 4 on August 23rd 2017.
* Date ranges: they are formatted as "dd.mm.yyyy - dd.mm.yyyy". Start 
and end date will be included.


### Counters ###
The counter IDs are entered as integers, starting from 0. 
Different counters can be used to measure how often the 
workers were active. The counters are one input which determines who 
is selected for a task. Different counters enable the creation of 
events whose activity does not matter for other events. 

An example use of different counters would be the following situation:
The overall group has two regular events A and B. A small subgroup 
has an additional event X. The people going to event X will 
automatically have high activity counters because there are only a 
few people who do tasks for event X. Assigning the tasks of event X
to a different counter makes sure that the people of the subgroup will
also do tasks at events A and B.




