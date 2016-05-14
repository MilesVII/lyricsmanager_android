package com.milesseventh.lyricsmanager;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.Mp3File;
import com.mpatric.mp3agic.NotSupportedException;
import com.mpatric.mp3agic.UnsupportedTagException;

public class Processor implements Runnable {
	public String mode;
	public Thread _t;
	public boolean active = true;
	private MainActivity friend = MainActivity.me;
	private int redir_amount = 0;
	
	Processor (String _mode){
		mode = _mode;
		_t = new Thread(this, "...");
		_t.start();
	}
	
	@Override
	public void run() {
		try{
			if (mode.equalsIgnoreCase(friend.COM_BURNDOWN)){
				for (File _unicorn : friend.selected){
					if (active){
						Mp3File _victim;
						_victim = new Mp3File (_unicorn);
						if (_victim.getId3v2Tag().getTitle() != null){
							_victim.getId3v2Tag().removeLyrics();
							friend.writeline(_unicorn.getName() + " has no lyrics now");
							_victim.save(_unicorn.getPath()+".x");
							overkill(_unicorn, new File (_unicorn.getPath()+".x"));
						} else
							friend.writeline(_unicorn.getName() + " : E: No ID3v2 tag or track title is null");
					}else{
						friend.writeline("Operation is interrupted");
						friend.ctxt = friend.CTXT_IDLE;
						return;
					}
				}
				friend.writeline("\nTheir blood is on your hooves. The work is done.");
				friend.ctxt = friend.CTXT_IDLE;
			}
			
			if (mode.equalsIgnoreCase(friend.COM_GL)){
				int _log_ok = 0, _log_nf = 0, _log_ex = 0, _log_proced = 0;;
				String _sizematters = "/" + Integer.toString(friend.selected.size()) + ". ";
				for (File _unicorn : friend.selected){
					if (active){
						_log_proced++;
						String _lyr = pullLyricsBind(_unicorn, true);
						redir_amount = 0;
						if (_lyr == "NF"){
							_log_nf++;
							friend.writeline(Integer.toString(_log_proced) + _sizematters + _unicorn.getName() + " : lyrics not found.");
						} else if (_lyr == "NT"){
							_log_nf++;
							friend.writeline(Integer.toString(_log_proced) + _sizematters + _unicorn.getName() + " : E: No ID3v2 tag or track title is null");
						} else if (_lyr.startsWith("EXIMAGIK:")){
							_log_ex++;
							friend.writeline(Integer.toString(_log_proced) + _sizematters + _unicorn.getName() + " : lyrics already exist. Ignored.");
						} else{
							_log_ok++;
							friend.writeline(Integer.toString(_log_proced) + _sizematters + _unicorn.getName() + " : lyrics downloaded and saved successfully.");
						}
					}else{
						friend.writeline("Operation is interrupted");
						friend.ctxt = friend.CTXT_IDLE;
						break;
					}
				}
				friend.writeline("\nDone!\nDownloaded: " + Integer.toString(_log_ok) + 
						 		 "\nIgnored due to existing lyrics: " + Integer.toString(_log_ex) + 
						 		 "\nNot found: " + Integer.toString(_log_nf) + 
						 		 "\n--Sum total: " + Integer.toString(_log_proced) + "\n");
				friend.ctxt = friend.CTXT_IDLE;
			}

			if (mode.startsWith(friend.COM_SL)){
				String _lyr, _mane;
				if (friend.show_holder == null){
					mode = mode.substring(friend.COM_SL.length());
					boolean _forcereg;
					if (mode.startsWith("!!")){
						mode = mode.substring(2);
						_forcereg = true;
					} else
						_forcereg = false;
					
					_mane = "";
					String[] _horse = mode.split(friend.Q_SPLT);
					if (mode.contains(friend.Q_SPLT)/* && _horse.length == 2 //Already checked before*/){
						_lyr = pullLyrics(_horse[0].trim(), _horse[1].trim(), 0, _forcereg);
						_mane = _horse[0].trim() + " " + friend.Q_SPLT + " " + _horse[1].trim();
					}else
						_lyr = "IE";
				} else {
					File _unicorn = friend.show_holder;
					_lyr = pullLyricsBind(_unicorn, false);
					_mane = _unicorn.getName();
				}
				if (_lyr == "IE")
					friend.writeline("\n" + _mane + " : Internal Error!\n");
				else if (_lyr == "NT")
					friend.writeline("\n" + _mane + " : No ID3v2 tag!\n");
				else if (_lyr == "NF")
					friend.writeline("\n" + _mane + " : lyrics not found.\n");
				else {
					if (_lyr.startsWith("EXIMAGIK:"))
						friend.writeline("\n" + _mane + " : lyrics already exist:\n" + _lyr.substring(9));
					else 
						friend.writeline("\n" + _mane + " : lyrics downloaded:\n" + _lyr);
					friend.copy_holder = _mane + "\n" + _lyr;
				}
				friend.ctxt = friend.CTXT_IDLE;
			}
			
			if (mode.startsWith(friend.COM_SEARCH)){
				String _res = "", _lyr, _query;
				_query = mode.substring(friend.COM_SEARCH.length());
				
				for (File _unicorn : friend.selected){
					if (active){
						Mp3File _victim = new Mp3File (_unicorn);
						_lyr = _victim.getId3v2Tag().getLyrics();
						if (_lyr != null){
							if (_lyr.toLowerCase().contains(_query))
								_res += "\n" + _unicorn.getName();
						}
					}else{
						friend.writeline("Operation is interrupted");
						break;
					}
				}
				
				if (_res == "")
					friend.writeline("\nNothing was found");
				else
					friend.writeline("\nText \"" + _query + "\" was found in following files:" + _res);
				friend.ctxt = friend.CTXT_IDLE;
			}
		}catch(Exception ex){
			friend.writeline("\nCRITICAL ERROR:" + ex.toString() + "\n" + ex.getMessage());
			friend.ctxt = friend.CTXT_IDLE;
		}
	}
	
	public String pullLyricsBind (File _unicorn, boolean writeintotag) throws UnsupportedTagException, InvalidDataException, IOException, NotSupportedException{
		Mp3File _victim = new Mp3File(_unicorn);
		ID3v2 _victimtag = _victim.getId3v2Tag();
		if(_victimtag.getTitle() == null)
			return("NT");
		boolean trywithoutparesis = false;
		if (_victimtag.getLyrics() == null){
			String santitle = _victimtag.getTitle();
			if (_victimtag.getTitle().contains("(") && _victimtag.getTitle().indexOf("(") != 0){
				santitle = _victimtag.getTitle().substring(0, _victimtag.getTitle().indexOf("(") - 1);
				trywithoutparesis = true;
			}
			String _lyr = pullLyrics(_victimtag.getArtist(), _victimtag.getTitle().replace('[', '(').replace(']', ')'), 0, false);
			if (_lyr == "NF" && trywithoutparesis){
				_lyr = pullLyrics(_victimtag.getArtist(), santitle.replace('[', '(').replace(']', ')'), 0, false);
			}
			if (_lyr != "NF" && _lyr != null)
				if (writeintotag){
					_victimtag.setLyrics(_lyr);
					_victim.save(_unicorn.getPath()+".x");
					overkill(_unicorn, new File (_unicorn.getPath()+".x"));
					return("OK");
				}else
					return(_lyr);//Lyrics downloaded
			else
				return("NF");//Lyrics not found
		}else
			return("EXIMAGIK:" + _victimtag.getLyrics());//Lyrics already exist
	}
	//http://inversekarma.in/technology/net/fetching-lyrics-from-lyricwiki-in-c/
	public String pullLyrics(String _artist, String _title, int depth, boolean _fg){
		if (depth >= 7){
			friend.writeline("Timeout. Please, try again later");
			return ("NF");
		}
		
		String _lyrics, _cleanurl;
		int iStart = 0;
		int iEnd = 0;
		String _rawquery = sanitize(_artist, _fg) + ":" + sanitize(_title, _fg);
		
		_cleanurl = "http://lyrics.wikia.com/index.php?title=";
		try {
			_cleanurl += URLEncoder.encode(_rawquery.split(":")[0], "UTF-8") +
						":" + URLEncoder.encode(_rawquery.split(":")[1], "UTF-8") + "&action=edit";
		} catch (UnsupportedEncodingException e) {
			friend.writeline("Error occured while encoding query string. Trying to use less safe method...");
			_cleanurl += _rawquery + "&action=edit";
		}
		_lyrics = pageDown(_cleanurl);
		
		//String downloading was interrupted
		if (!_lyrics.contains("</html>"))
			return (pullLyrics(_artist, _title, ++depth, _fg));
			
		//If Lyrics Wikia is suggesting a redirect, pull lyrics for that.
		if (_lyrics.contains("#REDIRECT")){
			if(redir_amount++ >= 3){//To be honest: I dont understand this kind of magic. Tho it doesnt matter
				friend.writeline("Error: Reached redirecton limit.");
				return ("NF");
			}
			
			iStart = _lyrics.indexOf("#REDIRECT [[") + 12;
			iEnd = _lyrics.indexOf("]]",iStart);
			_artist = _lyrics.substring(iStart, iEnd).split(":")[0];//slice() was here
			_title = _lyrics.substring(iStart, iEnd).split(":")[1].replace("&amp;", "&");//slice() was here
			friend.writeline("Query redirected to " + _artist + " - " + _title);
			return (pullLyrics(_artist, _title, 0, _fg));
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
	//Method replaces first letter of all words to UPPERCASE and replaces all spaces with underscores.
	private static String sanitize(String s, boolean _fg){
		char[] array = s.trim().toCharArray();
		if (!_fg){
			if (array.length >= 1 && Character.isLowerCase(array[0]))
					array[0] = Character.toUpperCase(array[0]);
			for (int i = 1; i < array.length; i++)
				if (array[i - 1] == ' ' && Character.isLowerCase(array[i]))
						array[i] = Character.toUpperCase(array[i]);
		}
		return new String(array).trim().replace(' ', '_')/*.replace("&", "%26")*/;
	}
	
	private void overkill(File _victim, File _master){
		_victim.delete();
		_master.renameTo(_victim);
	}
	
	protected void finalize() throws Throwable{
		friend.ctxt = friend.CTXT_IDLE;
		super.finalize();
	}
}
