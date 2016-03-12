package com.milesseventh.lyricsmanager;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Scanner;//Try to replace with String.split()
import java.lang.Thread;

import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.Mp3File;
import com.mpatric.mp3agic.UnsupportedTagException;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends Activity{
	private Button cd_b, com_b;
	private EditText cd_f, com_f;
	private TextView output;
	private String cur_path = "/storage";
	public int depth = 0;
	private File[] ls_list;
	public ArrayList<File> selected = new ArrayList<File>();
	public File show_holder;
	private String argument_holder;
	private Thread pageDowner;
	private boolean root_access_allowed = false;
	private Processor jack;
	public static MainActivity me;
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
		
		cd_f.setText(cur_path);
		ls_list = new File(cur_path).listFiles();
		
		cd_b.setOnClickListener(cd_listener);
		com_b.setOnClickListener(com_listener);
		
		writeline("\nApplication uses lyrics.wikia.com to fetch lyrics. " +
				  "If some lyrics weren't found or you've found any mistakes " + 
				  "in lyrics then be a good pony and fix them on the wiki's site.\n" + 
				  "Type 'help' into the second field to see the list of commands.");
	}
	
	private OnClickListener cd_listener = new OnClickListener(){
		public void onClick (View _fuckoff){
			cd_command(cd_f.getText().toString());
		}
	};
	
	private OnClickListener com_listener = new OnClickListener(){
		public void onClick (View _fuckoff){
			String _com = com_f.getText().toString().trim().toLowerCase();
			com_f.setText("");
			//Root access unlocking
			if (_com.equalsIgnoreCase("imnotsillyhorsy") && depth == 0){
				writeline("\nHope you know what you're doing. Access allowed.");
				root_access_allowed = true;
				return;
			}
			//CD command implementation
			if (_com.startsWith("cd") && depth == 0){
				//_com contains only argument
				_com = _com.substring(2).trim();
				int _hugme = firstArgument(_com);
				
				if (_hugme >= 0 && _hugme < ls_list.length){
					cd_command(ls_list[_hugme].getPath());
				}
				return;
			}
			//LS command implementation
			if (_com.equalsIgnoreCase("ls") && depth == 0){
				String _dirsep;
				for (int i = ls_list.length - 1; i >= 0; i--){
					_dirsep = ls_list[i].isDirectory()?"/":"";
					writeline(Integer.toString(i) + ". " + ls_list[i].getName() + _dirsep);
				}
				if (ls_list.length == 0)
					writeline("No files in " + cur_path);
				writeline("\n" + cur_path + " listing:");
				return;
			}
			//ADD command implementation
			if (_com.startsWith("add") && depth == 0){
				//_com contains only argument
				_com = _com.substring(3).trim();
				ArrayList<Integer> _sluice = processComArgument(_com);
				
				//converting collected nums to pathnames and adding them to selected
				int _submissive = selected.size();
				for (Integer _silliness : _sluice){
					if (_silliness >= 0 && _silliness < ls_list.length){
						if (!selected.contains(ls_list[_silliness]) && ls_list[_silliness].exists())
							if(ls_list[_silliness].isDirectory()){
								addSubFiles(ls_list[_silliness], selected);
							}else if (ls_list[_silliness].isFile() && ls_list[_silliness].getName().endsWith(".mp3")){//Send some mp3 to AVD plz
								selected.add(ls_list[_silliness]);
							}
					}else{
						writeline("E: Out of range. Position (" + Integer.toString(_silliness) + ") is larger than max position in current directory listing");
					}
				}
				writeline("\n" + Integer.toString(selected.size() - _submissive) + " entries added to list");
				return;
			}
			//SHOW command implementation
			if (_com.startsWith("show") && depth == 0){
				if (_com.equalsIgnoreCase("show")){
					for (int i = selected.size() - 1; i >= 0; i--){
						writeline(Integer.toString(i) + ". " + selected.get(i).getName());
					}
					if (selected.size() == 0)
						writeline("No files selected");
					writeline("\nSelection listing:");
				}else{
					_com = _com.substring(4).trim();
					ArrayList<Integer> _sluice = processComArgument(_com);
					for (Integer _pony : _sluice){
						if (_pony >= 0 && _pony < selected.size())
							writeline(Integer.toString(_pony) + ". " + selected.get(_pony));
					}
					writeline("\nDetailed selection listing:");
				}
				return;
			}
			//CLEAR command implementation
			if (_com.equalsIgnoreCase("clear") && depth == 0){
				writeline(selected.size() + " entries will be deselected. Continue? Y/N");
				depth = 1;
				return;
			}
			if (depth == 1){
				if (_com.charAt(0) == 'y'){
					selected.clear();
					writeline("\nSelection cleared");
					depth = 0;
				}else if (_com.charAt(0) == 'n'){
					writeline("\nOkay, selection is untouched");
					depth = 0;
				}else
					writeline("Question is the same");
				return;
			}
			//REMOVE command implementation
			if (_com.startsWith("remove") && depth == 0){
				_com = _com.substring(6).trim();
				argument_holder = _com;
				writeline("These entries will be removed from selection list. Continue? Y/N");
				depth = 2;
				return;
			}
			if (depth == 2){
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
					writeline("\nRemoving is complete. Use 'show' to check any changes");
					depth = 0;
				}else if (_com.charAt(0) == 'n'){
					writeline("\nOkay, selection is untouched");
					depth = 0;
				}else
					writeline("Question is the same");
				return;
			}
			//HELP command implementation
			if ((_com.startsWith("help") || _com.startsWith("man") || _com.startsWith("pony")) && depth == 0){
				writeline("\nclearconsole : clear console");
				writeline("abort : stop current operation");
				writeline("gl or getlyrics : download and write lyrics into tags of selected files");
				writeline("loadlyrics n : download and show (but not write into tag) lyrics for the one of selected files");
				writeline("burndown : remove all lyrics from tags of selected files");
				writeline("clear : reset selection");
				writeline("remove n,n-n : remove some entries from selection");
				writeline("show n,n-n : show selected files and their paths");
				writeline("show : show selected files");
				writeline("add n,n-n : add files in n-positions and position ranges to selection");
				writeline("cd n : open directory in position n");
				writeline("ls : list files in current directory");
				writeline("List of commands:");
				return;
			}
			//BURNDOWN command implementation
			if (_com.equalsIgnoreCase("burndown") && depth == 0){
				if (selected.size() > 0){
					writeline("Lyrics of selected files will be erased. Say 'determined' to continue.");
					depth = 3;
				}else{
					writeline("\nE: No files selected");
				}
				return;
			}
			if (depth == 3){
				if (_com.equalsIgnoreCase("determined")){
					depth = 7;
					writeline("Say your last prayer you worthless filthy lyrics! " + 
							  "Ashes is your name and you will fall! Raaaawwrrrr!!!");
					jack = new Processor ("burndown");
				}else{
					depth = 0;
					writeline("\nOperation aborted. Selected files are untouched.");
				}
				return;
			}
			//GETLYRICS command implementation
			if ((_com.equalsIgnoreCase("gl") || _com.equalsIgnoreCase("getlyrics")) && depth == 0){
				if (selected.size() > 0){
					writeline("Operation is started");
					depth = 7;
					jack = new Processor("getlyrics");
				}else{
					writeline("\nE: No files selected");
				}
				return;
			}
			//LOADLYRICS command implementation
			if (!_com.equalsIgnoreCase("loadlyrics") && _com.startsWith("loadlyrics") && depth == 0){
				if (selected.size() == 0){
					writeline("\nE: No files selected");
					return;
				}
				
				_com = _com.substring(10);
				int _kissme = firstArgument(_com);
				if (_kissme >= 0 && _kissme < selected.size()){
					show_holder = selected.get(_kissme);
					writeline("Loading...");
					jack = new Processor("showlyrics");
				}
				return;
			}
			//ABORT command implementation
			if (_com.equalsIgnoreCase("abort") && depth == 7){
				if (jack != null){
					writeline("\nAborted.");
					jack._t.interrupt();
					try {
						jack.finalize();
					} catch (Throwable e) {
					}
					depth = 0;
				}
				return;
			}
			//CLEARCONSOLE command implementation
			if (_com.equalsIgnoreCase("clearconsole")){
				output.setText("");
			}
			//SHOWCONTEXT command implementation
			if (_com.equalsIgnoreCase("showcontext")){
				writeline(Integer.toString(depth));
			}
		}
	};
	
	private void addSubFiles (File _dir, ArrayList<File> _receiver){
		File[] _lick = _dir.listFiles();
		for (File _saliva : _lick){
			if(_saliva.isDirectory()){
				addSubFiles(_saliva, _receiver);
			}else if (_saliva.isFile() && _saliva.getName().endsWith(".mp3") && !_receiver.contains(_saliva) && _saliva.exists()){
				_receiver.add(_saliva);
			}
		}
	}
	
	private ArrayList<Integer> processComArgument (String _argument){
		ArrayList<String> _sluice = new ArrayList<String>();
		ArrayList<String> _holder = new ArrayList<String>();//Fail-fast avoider
		ArrayList<Integer> _result = new ArrayList<Integer>();

		Scanner	_unicorn = new Scanner(_argument).useDelimiter("\\s*,\\s*");
		while(_unicorn.hasNext()){
			_sluice.add(_unicorn.next());
		}
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
		
		if (_sluice.size() > 0){
			return (_sluice.get(0));
		}else{
			return (-1);
		}
	}
	
	private void cd_command(String _to){
		File _t = new File(_to);
		if (_t.exists() && _t.isDirectory())
			if (!_t.getPath().equalsIgnoreCase("/") || root_access_allowed){
				cur_path = _to;
				ls_list = _t.listFiles();
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