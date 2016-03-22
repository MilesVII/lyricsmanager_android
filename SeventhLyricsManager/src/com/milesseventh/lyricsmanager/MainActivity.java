package com.milesseventh.lyricsmanager;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Scanner;//Try to replace with String.split()

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends Activity{
	//Command names
	public final String COM_LS = "ls";
	public final String COM_CD = "cd";
	public final String COM_ADD = "add";
	public final String COM_SELECTION = "selection";
	public final String COM_REMOVE = "remove";
	public final String COM_CLEAR = "clear";
	public final String COM_BURNDOWN = "burndown";
	public final String COM_SL = "showlyrics";
	public final String COM_GL = "getlyrics";
	public final String COM_ALTGL = "gl";
	public final String COM_SEARCH = "search";
	public final String COM_ABORT = "abort";
	public final String COM_COPY = "copy";
	public final String COM_CLEARCONSOLE = "clearconsole";
	public final String COM_HELP = "help";
	public final String COM_ABOUT = "about";

	public final int CTXT_IDLE = 0;
	public final int CTXT_CLEARQ = 1;
	public final int CTXT_REMOVEQ = 2;
	public final int CTXT_BURNQ = 3;
	public final int CTXT_BUSY = 7;
	
	private Button cd_b, com_b;
	private EditText cd_f, com_f;
	private TextView output;
	private String cur_path = "/storage";
	public int ctxt = CTXT_IDLE;
	private File[] ls_list;
	public ArrayList<File> selected = new ArrayList<File>();
	public File show_holder;
	private String argument_holder;
	private boolean root_access_allowed = false;
	private Processor jack;
	private ClipboardManager clipboard;
	public static MainActivity me;
	public String copy_holder;
	private Comparator ls_comp = new Comparator(){
		public int compare(Object o1, Object o2){
			File f1 = (File) o1;
			File f2 = (File) o2;
			if(f1.isDirectory() && !f2.isDirectory())
				return -1;
			else if (!f1.isDirectory() && f2.isDirectory())
				return 1;
			else
				return f1.compareTo(f2);
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		me = this;
		
		cd_b = (Button) findViewById(R.id.cd_button);
		com_b = (Button) findViewById(R.id.enter_button);
		cd_f = (EditText) findViewById(R.id.path_field);
		com_f = (EditText) findViewById(R.id.command_field);
		output = (TextView) findViewById(R.id.output);
		clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
		
		cd_f.setText(cur_path);
		ls_list = new File(cur_path).listFiles();
		
		cd_b.setOnClickListener(new OnClickListener(){
			public void onClick (View view_chan){
				cd_command(cd_f.getText().toString());
			}
		});
		com_b.setOnClickListener(com_listener);
		
		writeline("\nApplication uses http://lyrics.wikia.com to fetch lyrics. " +
				  "If some lyrics weren't found or you've found any mistakes " + 
				  "in lyrics then be a good pony and fix them on the wiki's site.\n" + 
				  "Type 'help' into the second field to see the list of commands.");
	}
	
	private OnClickListener com_listener = new OnClickListener(){
		public void onClick (View view_chan){
			String _com = com_f.getText().toString().trim().toLowerCase();
			if(_com.length() == 0)
				return;
			com_f.setText("");
			//Root access unlocking
			if (_com.equalsIgnoreCase("imnotsillyhorsy") && ctxt == CTXT_IDLE){
				writeline("\nHope you know what you're doing. Access allowed.");
				root_access_allowed = true;
				return;
			}
			//CD command implementation
			if (_com.startsWith(COM_CD) && ctxt == CTXT_IDLE){
				//_com contains only argument
				_com = _com.substring(COM_CD.length()).trim();
				int _hugme = firstArgument(_com);
				
				if (_hugme >= 0 && _hugme < ls_list.length)
					cd_command(ls_list[_hugme].getPath());
				return;
			}
			//LS command implementation
			if (_com.equalsIgnoreCase(COM_LS) && ctxt == CTXT_IDLE){
				for (int i = ls_list.length - 1; i >= 0; i--)
					writeline(Integer.toString(i) + ". " + ls_list[i].getName() + ( ls_list[i].isDirectory()?"/":""));
				if (ls_list.length == 0)
					writeline("No files in " + cur_path);
				writeline("\n" + cur_path + " listing:");
				return;
			}
			//ADD command implementation
			if (_com.startsWith(COM_ADD) && ctxt == CTXT_IDLE){
				//_com contains only argument
				_com = _com.substring(COM_ADD.length()).trim();
				ArrayList<Integer> _sluice = processComArgument(_com);
				
				//converting collected nums to pathnames and adding them to selected
				int _submissive = selected.size();
				for (Integer _silliness : _sluice){
					if (_silliness >= 0 && _silliness < ls_list.length)
						if (!selected.contains(ls_list[_silliness]) && ls_list[_silliness].exists())
							if(ls_list[_silliness].isDirectory())
								addSubFiles(ls_list[_silliness], selected);
							else if (ls_list[_silliness].isFile() && ls_list[_silliness].getName().endsWith(".mp3"))
								selected.add(ls_list[_silliness]);
					else
						writeline("E: Out of range. Position (" + Integer.toString(_silliness) + 
								  ") is larger than max position in current directory listing");
				}
				writeline("\n" + Integer.toString(selected.size() - _submissive) + " entries added to list");
				return;
			}
			//SELECTION command implementation
			if (_com.startsWith(COM_SELECTION) && ctxt == CTXT_IDLE){
				if (_com.equalsIgnoreCase(COM_SELECTION)){
					for (int i = selected.size() - 1; i >= 0; i--)
						writeline(Integer.toString(i) + ". " + selected.get(i).getName());
					if (selected.size() == 0)
						writeline("No files selected");
					writeline("\nSelection listing:");
				}else{
					_com = _com.substring(COM_SELECTION.length()).trim();
					ArrayList<Integer> _sluice = processComArgument(_com);
					for (Integer _pony : _sluice)
						if (_pony >= 0 && _pony < selected.size())
							writeline(Integer.toString(_pony) + ". " + selected.get(_pony));
					writeline("\nDetailed selection listing:");
				}
				return;
			}
			//CLEAR command implementation
			if (_com.equalsIgnoreCase(COM_CLEAR) && ctxt == CTXT_IDLE){
				writeline(selected.size() + " entries will be deselected. Continue? Y/N");
				ctxt = CTXT_CLEARQ;
				return;
			}
			if (ctxt == CTXT_CLEARQ){
				if (_com.charAt(0) == 'y'){
					selected.clear();
					writeline("\nSelection cleared");
					ctxt = CTXT_IDLE;
				}else if (_com.charAt(0) == 'n'){
					writeline("\nOkay, selection is untouched");
					ctxt = CTXT_IDLE;
				}else
					writeline("Question is the same");
				return;
			}
			//REMOVE command implementation
			if (_com.startsWith(COM_REMOVE) && ctxt == CTXT_IDLE){
				_com = _com.substring(COM_REMOVE.length()).trim();
				argument_holder = _com;
				writeline("These entries will be removed from selection list. Continue? Y/N");
				ctxt = CTXT_REMOVEQ;
				return;
			}
			if (ctxt == CTXT_REMOVEQ){
				if (_com.charAt(0) == 'y'){
					ArrayList<Integer> _execution_list = processComArgument(argument_holder);
					ArrayList<File> _death_surrounds = new ArrayList<File>();//Fast-fail avoider
					_death_surrounds.addAll(selected);
					for (Integer _bullet : _execution_list){
						if (_bullet >= 0 && _bullet < selected.size()){
							_death_surrounds.remove(selected.get(_bullet));
							writeline(selected.get(_bullet).getName() + " is not selected now");
						}
					}
					selected = _death_surrounds;
					writeline("\nRemoving is complete. Use '" + COM_SELECTION + "' to check any changes");
					ctxt = CTXT_IDLE;
				}else if (_com.charAt(0) == 'n'){
					writeline("\nOkay, selection is untouched");
					ctxt = CTXT_IDLE;
				}else
					writeline("Question is the same");
				return;
			}
			//HELP command implementation
			if (_com.startsWith(COM_HELP) && ctxt == CTXT_IDLE){
				writeline("\nList of commands:\n" +
						  COM_LS + " : list files in current directory;\n" +
						  COM_CD + " n : open directory in position n\n" +
						  COM_ADD + " n,n-n : add files in n-positions and position ranges to selection;\n" +
						  COM_SELECTION + " : show selected files;\n" +
						  COM_SELECTION + " n,n-n : show selected files and their paths;\n" +
						  COM_REMOVE + " n,n-n : remove some entries from selection;\n" +
						  COM_CLEAR + " : reset selection;\n" +
						  COM_BURNDOWN + " : remove all lyrics from tags of selected files;\n" +
						  COM_SL + " n : download and show (but not write into tag) lyrics for the one of selected files;\n" +
						  COM_SL + " artist;title : download and show lyrics of selected song. Using example: \"showlyrics helmet;crashing foreign cars\";\n" +
						  COM_GL + " or " + COM_ALTGL + " : download and write lyrics into tags of selected files;\n" +
						  COM_SEARCH + " searchquery: search for 'searchquery' in ID3v2 tag of selected files;\n" +
						  COM_ABORT + " : stop current operation;\n" +
						  COM_COPY + " : save the last showed lyrics to clipboard;\n" +
						  COM_CLEARCONSOLE + " : clear console;\n" +
						  COM_ABOUT + " : show application info;");
				return;
			}
			//BURNDOWN command implementation
			if (_com.equalsIgnoreCase(COM_BURNDOWN) && ctxt == CTXT_IDLE){
				if (selected.size() > 0){
					writeline("Lyrics of selected files will be erased. Say 'determined' to continue.");
					ctxt = CTXT_BURNQ;
				}else
					writeline("\nE: No files selected");
				return;
			}
			if (ctxt == CTXT_BURNQ){
				if (_com.equalsIgnoreCase("determined")){
					ctxt = CTXT_BUSY;
					writeline("Say your last prayer you worthless filthy lyrics! " + 
							  "Ashes is your name and you will fall! Raaaawwrrrr!!!");
					jack = new Processor (COM_BURNDOWN);
				}else{
					ctxt = CTXT_IDLE;
					writeline("\nOperation aborted. Selected files are untouched.");
				}
				return;
			}
			//GETLYRICS command implementation
			if ((_com.equalsIgnoreCase(COM_GL) || _com.equalsIgnoreCase(COM_ALTGL)) && ctxt == CTXT_IDLE){
				if (selected.size() > 0){
					writeline("Operation is started");
					ctxt = CTXT_BUSY;
					jack = new Processor(COM_GL);
				}else
					writeline("\nE: No files selected");
				return;
			}
			//SHOWLYRICS command implementation
			if (!_com.equalsIgnoreCase(COM_SL) && _com.startsWith(COM_SL) && ctxt == CTXT_IDLE){
				_com = _com.substring(COM_SL.length()).trim();
				if (_com.contains(";")){
					if (_com.split(";").length == 2){
						_com = _com.toLowerCase();
						show_holder = null;
						writeline("Loading...");
						jack = new Processor(COM_SL + _com.split(";")[0].trim() + ";" + _com.split(";")[1].trim());
					}
				} else {
					if (selected.size() == 0){
						writeline("\nE: No files selected");
						return;
					}
					
					int _kissme = firstArgument(_com);
					writeline(Integer.toString(_kissme));
					if (_kissme >= 0 && _kissme < selected.size()){
						show_holder = selected.get(_kissme);
						writeline("Loading...");
						jack = new Processor(COM_SL);
					}
				}
				return;
			}
			//ABORT command implementation
			if (_com.equalsIgnoreCase(COM_ABORT) && ctxt == CTXT_BUSY){
				if (jack != null)
					jack.active = false;
				return;
			}
			//CLEARCONSOLE command implementation
			if (_com.equalsIgnoreCase(COM_CLEARCONSOLE)){
				output.setText("");
			}
			//ABOUT command implementation
			if (_com.equalsIgnoreCase(COM_ABORT) && ctxt == CTXT_IDLE){
				writeline("\n______________\n" + 
						  "Seventh Lyrics Manager is free opensource application " + 
						  "that allows you to automagically download and write " + 
						  "song lyrics into ID3v2 tag that included by the most of MP3 " + 
						  "files. There is a lot of music players which are able " + 
						  "to show song's lyric while playing. \n" +
						  "Designed by Miles Seventh, 2016\n\n" +
						  "Disclaimer:\n" + 
						  "Seventh Lyrics Manager is provided by Miles Seventh \"as is\" " + 
						  "and \"with all faults\". Developer makes no representations or " + 
						  "warranties of any kind concerning the safety, suitability, " + 
						  "lack of viruses, inaccuracies, typographical errors, or other " + 
						  "harmful components of this software. You are solely " + 
						  "responsible for the protection of your equipment and backup " + 
						  "of your data, and the Developer will not be liable for any " + 
						  "damages you may suffer in connection with using, modifying, " + 
						  "or distributing this software.\n______________");
			}
			//COPY command implementation
			if (_com.equalsIgnoreCase(COM_COPY) && ctxt == CTXT_IDLE && copy_holder != null){
				clipboard.setPrimaryClip(ClipData.newPlainText("Lyrics", copy_holder));
				writeline("\nLast showed lyrics was successfully copied to clipboard");
				return;
			}
			//SEARCH command implementation
			if (_com.startsWith(COM_SEARCH) && ctxt == CTXT_IDLE){
				_com = _com.substring(COM_SEARCH.length()).trim();
				if (selected.size() == 0 || _com.length() == 0)
					return;
				writeline("Searching...");
				jack = new Processor(COM_SEARCH + _com);
				return;
			}
		}
	};
	
	private void addSubFiles (File _dir, ArrayList<File> _receiver){
		File[] _lick = _dir.listFiles();
		for (File _saliva : _lick)
			if(_saliva.isDirectory())
				addSubFiles(_saliva, _receiver);
			else if (_saliva.isFile() && _saliva.getName().endsWith(".mp3") && 
					!_receiver.contains(_saliva) && _saliva.exists())
				_receiver.add(_saliva);
	}
	
	private ArrayList<Integer> processComArgument (String _argument){
		ArrayList<String> _sluice = new ArrayList<String>();
		ArrayList<String> _holder = new ArrayList<String>();//Fail-fast avoider
		ArrayList<Integer> _result = new ArrayList<Integer>();

		Scanner	_unicorn = new Scanner(_argument).useDelimiter("\\s*,\\s*");
		while(_unicorn.hasNext())
			_sluice.add(_unicorn.next());
		_holder.addAll(_sluice);
		for (String _neck : _holder){
			if (_neck.contains("-")){
				try{
					int _l = Integer.parseInt(_neck.substring(0, _neck.indexOf("-")));
					int _r = Integer.parseInt(_neck.substring(_neck.indexOf("-") + 1, _neck.length()));
					if (_l > _r){
						int _z = _l; _l = _r; _r = _z;
					}
					for (int i = _l; i <= _r; i++)
						_sluice.add(Integer.toString(i));
				}
				catch (NumberFormatException ex){
					writeline("E: Error parsing '" + _neck + "'. Expression ignored");
				}
				finally{
					_sluice.remove(_neck);
				}
			}
		}
		
		for (String _neck : _sluice){
			try{
				_result.add(Integer.parseInt(_neck));
			}
			catch(NumberFormatException ex){
				writeline("E: Error parsing '" + _neck + "'. Expression ignored");
			}
		}
		return (_result);
	}
	
	private int firstArgument(String _com){
		ArrayList<Integer> _sluice = processComArgument(_com);
		
		if (_sluice.size() > 0)
			return (_sluice.get(0));
		else
			return (-1);
	}
	
	private void cd_command(String _to){
		File _t = new File(_to);
		if (_t.exists() && _t.isDirectory())
			if (!_t.getPath().equalsIgnoreCase("/") || root_access_allowed){
				cur_path = _to;
				ls_list = _t.listFiles(new FileFilter(){
					@Override
					public boolean accept(File pathname){
						if(pathname.isDirectory())
							return true;
						else 
							return pathname.getName().endsWith(".mp3");
					}
				});
				Arrays.sort(ls_list, ls_comp);
				cd_f.setText(cur_path);
				writeline("\ncd: " + cur_path);
			}else
				writeline("\nE: Root directory is unaccessible");
		else
			writeline("\nE: Path not found");
	}
	
	public synchronized void writeline(final String _pony){
		super.runOnUiThread(new Runnable(){
			public void run(){
				output.setText(_pony + "\n" + output.getText());
			}
		});
	}
}