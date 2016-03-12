package com.milesseventh.lyricsmanager;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.Mp3File;
import com.mpatric.mp3agic.NotSupportedException;
import com.mpatric.mp3agic.UnsupportedTagException;

public class Processor implements Runnable {
	public String mode, log;
	public Thread _t;
	private MainActivity friend = MainActivity.me;
	
	Processor (String _mode){
		mode = _mode;
		_t = new Thread(this, "...");
		_t.start();
	}
	
	@Override
	public void run() {
		if (mode.equalsIgnoreCase("burndown")){
			for (File _unicorn : friend.selected){
				Mp3File _victim;
				try {
					_victim = new Mp3File (_unicorn);
					_victim.getId3v2Tag().removeLyrics();
					friend.writeline(_unicorn.getName() + " has no lyrics now");
					_victim.save(_unicorn.getPath()+".x");
					overkill(_unicorn, new File (_unicorn.getPath()+".x"));
				} catch (UnsupportedTagException e) {
				} catch (InvalidDataException e) {
				} catch (IOException e) {
				} catch (NotSupportedException e) {
				}
			}
			friend.writeline("\nTheir blood is on your hooves. The work is done.");
			friend.depth = 0;
		}
		
		if (mode.equalsIgnoreCase("getlyrics")){
			int _complete = 0;
			String _sizematters = "/" + Integer.toString(friend.selected.size()) + ". ";
			for (File _unicorn : friend.selected){
				_complete++;
				try {
					Mp3File _victim = new Mp3File(_unicorn);
					ID3v2 _victimtag = _victim.getId3v2Tag();
					boolean trywithoutparesis = false;
					if (_victimtag.getLyrics() == null){
						String santitle = _victimtag.getTitle();
						if (_victimtag.getTitle().contains("(")){
							santitle = _victimtag.getTitle().substring(0, _victimtag.getTitle().indexOf("(") - 1);
							trywithoutparesis = true;
						}
						String _lyr = pullLyrics(_victimtag.getArtist(), _victimtag.getTitle(), 0);
						if (_lyr == "NF" && trywithoutparesis){
							_lyr = pullLyrics(_victimtag.getArtist(), santitle, 0);
							trywithoutparesis = false;
						}
						if (_lyr != "NF" && _lyr != null){
							_victimtag.setLyrics(_lyr);
							_victim.save(_unicorn.getPath()+".x");
							overkill(_unicorn, new File (_unicorn.getPath()+".x"));
							friend.writeline(Integer.toString(_complete) + _sizematters + _unicorn.getName() + " : lyrics downloaded and saved successfully.");
						}else{
							friend.writeline(Integer.toString(_complete) + _sizematters + _unicorn.getName() + " : lyrics not found.");
						}
					}else{
						friend.writeline(Integer.toString(_complete) + _sizematters + _unicorn.getName() + " : lyrics already exist. Ignored.");
					}
				} catch (UnsupportedTagException e) {} catch (InvalidDataException e) {} catch (IOException e) {} catch (NotSupportedException e) {}
			}
			friend.writeline("\nDone!");
			friend.depth = 0;
		}
		
		if (mode.equalsIgnoreCase("showlyrics")){
			int _complete = 0;
			File _unicorn = friend.show_holder;
			try {
				Mp3File _victim = new Mp3File(_unicorn);
				ID3v2 _victimtag = _victim.getId3v2Tag();
				boolean trywithoutparesis = false;
				if (_victimtag.getLyrics() == null){
					String santitle = _victimtag.getTitle();
					if (_victimtag.getTitle().contains("(")){
						santitle = _victimtag.getTitle().substring(0, _victimtag.getTitle().indexOf("(") - 1);
						trywithoutparesis = true;
					}
					String _lyr = pullLyrics(_victimtag.getArtist(), _victimtag.getTitle(), 0);
					if (_lyr == "NF" && trywithoutparesis){
						_lyr = pullLyrics(_victimtag.getArtist(), santitle, 0);
						trywithoutparesis = false;
					}
					if (_lyr != "NF" && _lyr != null){
						friend.writeline("\n" + _unicorn.getName() + " : lyrics downloaded:\n" + _lyr);
					}else{
						friend.writeline("\n" + _unicorn.getName() + " : lyrics not found.\n");
					}
				}else{
					friend.writeline("\n" + _unicorn.getName() + " : lyrics already exist: " + _victimtag.getLyrics() + "\n");
				}
			} catch (UnsupportedTagException e) {} catch (InvalidDataException e) {} catch (IOException e) {}
			
			friend.depth = 0;
		}
	}
	
	
	
	public String pageDown(String _url){
	    String line = "", all = "";
	    URL myUrl = null;
	    BufferedReader in = null;
	    try {
	        myUrl = new URL(_url);
	        in = new BufferedReader(new InputStreamReader(myUrl.openStream()));

	        while ((line = in.readLine()) != null) {
	            all += line + "\n";
	        }
	    } catch (MalformedURLException e) {} catch (IOException e) {} 
	    finally {
	        if (in != null) {
	            try {
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
	        }
	    }
	    return (all);
	}
	
	//http://inversekarma.in/technology/net/fetching-lyrics-from-lyricwiki-in-c/
	public String pullLyrics(String _artist, String _title, int depth){
		if (depth >= 7)
			return ("NF");
		
		String _lyrics;
		int iStart = 0;
		int iEnd = 0;
		
		_lyrics = pageDown("http://lyrics.wikia.com/index.php?title=" + sanitize(_artist) + ":" + sanitize(_title) + "&action=edit");
		
		//String downloading was interrupted
		if (!_lyrics.contains("</html>"))
			return (pullLyrics(_artist, _title, depth++));
			
		//If Lyrics Wikia is suggesting a redirect, pull lyrics for that.
		if (_lyrics.contains("#REDIRECT")){
			iStart = _lyrics.indexOf("#REDIRECT [[") + 12;
			iEnd = _lyrics.indexOf("]]",iStart);
			_artist = _lyrics.substring(iStart, iEnd).split(":")[0];//slice() was here
			_title = _lyrics.substring(iStart, iEnd).split(":")[1];//slice() was here
			return (pullLyrics(_artist, _title, 0));
		} else if (_lyrics.contains("!-- PUT LYRICS HERE (and delete this entire line) -->"))//Lyrics not found
			return ("NF");
		
		//Get surrounding tags.
		iStart = _lyrics.indexOf("&lt;lyrics>") + 11;
		iEnd = _lyrics.indexOf("&lt;/lyrics>") - 1;

		//Strange megarare shit happened.
		if(iStart == 10 || iEnd == -2){
			return ("NF");
		}
		
		return (_lyrics.substring(iStart, iEnd).trim().replace("&amp;", "&"));
	}
	
	//Method replaces first letter of all words to UPPERCASE and replaces all spaces with underscores.
	private static String sanitize(String s){
		char[] array = s.trim().toCharArray();
		if (array.length >= 1){
			if (Character.isLowerCase(array[0])){
				array[0] = Character.toUpperCase(array[0]);
			}
		}
		for (int i = 1; i < array.length; i++){
			if (array[i - 1] == ' '){
				if (Character.isLowerCase(array[i])){
					array[i] = Character.toUpperCase(array[i]);
				}
			}
		}
		return new String(array).trim().replace(' ', '_').replace("&", "%26");
	}
	
	private void overkill(File _victim, File _master){
		//String _blood = _victim.getName();
		_victim.delete();
		_master.renameTo(_victim);
	}
	
	protected void finalize() throws Throwable{
		friend.depth = 0;
		super.finalize();
	}
}
