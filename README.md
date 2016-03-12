Seventh Lyrics Manager

MP3 console lyrics manager for Android. Downloads lyrics and automatically writes them into ID3 tag. 
Similar app is "Music Tagger" by Deos Apps.

Brief manual:
To see the list of available commands type "help" into the second field and press "Enter".
Firstly, you need to select some files to process them. To do that you can use "ls" and "cd" commands or the very first field on the screen. If you remember the path where your files are then just type that path into the first field and tap "cd" button. Else just type "ls" into the second field and tap "Enter". You will see the list of directories and files in the current directory like this:</br>
0. firstDirectory/</br>
1. secondDir/</br>
2. track 01.mp3</br>
3. track 77.mp3</br>
4. sotw.mp3</br>
5. untitled.mp3</br>
6. file.mp3</br>
7. track 9.mp3</br>

Then type "cd n", where n is the number of directory that shown on the left side of the directory name in ls-listing. For example, if you type "cd 0" you will open "firstDirectory" folder.
When you've opened necessary path, use "add" command. For example, you can type "add 1,5-7,3" and select 1,3,5,6,7 position from the list. You can see that there is a directory at the position one. So, when you add directory to selection, application will add all mp3-files that will be found in this directory and subdirectories.
Use command "show" to see selected files. If you want to check paths of the selected files, use "show n,n-n", where "n" are positions of necessary entries in show-listing.
To remove some wrong entries from selection, use "remove n,n-n" command, where "n" are positions of necessary entries in show-listing. Also, you can reset the selection using "clear" command.
Use command "getlyrics" or "gl" to process formed queue and download lyrics. Also, you can erase lyrics using "burndown" command.
