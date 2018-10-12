# acmelog

LOG search and stats extract<br>

required program arguments are:<br>

-j <"SEARCH" | "EXTRACT"> (job)<br>
-s <File> (source)<br>

optional arguments:<br><br>
-f <LOCALDATETIME> (from date) format 2018-10-25T01:17:59<br>
-t <LOCALDATETIME> (to date) format 2018-10-25T01:17:59<br>
-d <File> (destination)<br><br>
-v <"INFO,WARN,ERROR"> (severity/ies)<br>
-w <words | phrases> (contain words)<br>

ie: 
-j SEARCH -s temp/all-logs.txt -d temp/result.txt -f 2018-10-25T01:17:59 -t 2018-10-39T01:17:59 -v ERROR,WARN -w logged in,user
