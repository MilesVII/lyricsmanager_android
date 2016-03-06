package com.milesseventh.lyricsmanager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Scanner;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.Arrays;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.mpatric.mp3agic.*;

public class MainActivity extends Activity {
	private Button cd_b, com_b;
	private EditText cd_f, com_f;
	private TextView output;
	private String cur_path = "/storage";
	private int depth = 0;
	private File[] ls_list;
	private ArrayList<File> selected = new ArrayList<File>();
	private String argument_holder;
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

		cd_b = (Button) findViewById(R.id.cd_button);
		com_b = (Button) findViewById(R.id.enter_button);
		cd_f = (EditText) findViewById(R.id.path_field);
		com_f = (EditText) findViewById(R.id.command_field);
		output = (TextView) findViewById(R.id.output);
		
		cd_f.setText(cur_path);
		ls_list = new File(cur_path).listFiles();
		
		cd_b.setOnClickListener(cd_listener);
		com_b.setOnClickListener(com_listener);
	}
	
	private OnClickListener cd_listener = new OnClickListener(){
		public void onClick (View _fuckoff){
			File _t = new File(cd_f.getText().toString());
			if (_t.exists() && _t.isDirectory()){
				cur_path = cd_f.getText().toString();
				ls_list = _t.listFiles();
				Arrays.sort(ls_list, ls_comp);
				writeline("cd " + cur_path);
			}else{
				writeline("E: Path not found");
			}
		}
	};
	
	private OnClickListener com_listener = new OnClickListener(){
		public void onClick (View _fuckoff){
			String _com = com_f.getText().toString().trim().toLowerCase();
			com_f.setText("");
			//LS command implementation
			if (_com.equalsIgnoreCase("ls") && depth == 0){
				writeline("");
				writeline("__________");
				String _dirsep;
				for (int i = ls_list.length - 1; i >= 0; i--){
					_dirsep = ls_list[i].isDirectory()?"/":"";
					writeline(Integer.toString(i) + ". " + ls_list[i].getName() + _dirsep);
				}
				if (ls_list.length == 0)
					writeline("No files in " + cur_path);
				writeline(cur_path + " listing:");
				return;
			}
			//ADD command implementation
			if (_com.startsWith("add") && depth == 0){
				writeline("");
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
				writeline(Integer.toString(selected.size() - _submissive) + " entries added to list");
				return;
			}
			//SHOW command implementation
			if (_com.startsWith("show") && depth == 0){
				if (_com.equalsIgnoreCase("show")){
					writeline("");
					writeline("__________");
					for (int i = selected.size() - 1; i >= 0; i--){
						writeline(Integer.toString(i) + ". " + selected.get(i).getName());
					}
					if (selected.size() == 0)
						writeline("No files selected");
					writeline("Selection listing:");
				}else{
					_com = _com.substring(4).trim();
					ArrayList<Integer> _sluice = processComArgument(_com);

					writeline("");
					writeline("__________");
					for (Integer _pony : _sluice){
						if (_pony >= 0 && _pony < selected.size())
							writeline(Integer.toString(_pony) + ". " + selected.get(_pony));
					}
					writeline("Detailed selection listing:");
				}
				return;
			}
			//CLEAR command implementation
			if (_com.equalsIgnoreCase("clear") && depth == 0){
				writeline(selected.size() + " entries will be deselected. Continue? Y/N\n");
				depth = 1;
				return;
			}
			if (depth == 1){
				if (_com.charAt(0) == 'y'){
					selected.clear();
					writeline("Selection cleared\n");
					depth = 0;
				}else if (_com.charAt(0) == 'n'){
					writeline("Okay, selection is untouched\n");
					depth = 0;
				}else
					writeline("Question is the same");
			}
			//REMOVE command implementation
			if (_com.startsWith("remove") && depth == 0){
				_com = _com.substring(6).trim();
				argument_holder = _com;
				writeline("These entries will be removed from selection list. Continue? Y/N\n");
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
					writeline("Removing is complete. Use 'show' to check any changes");
					depth = 0;
				}else if (_com.charAt(0) == 'n'){
					writeline("Okay, selection is untouched\n");
					depth = 0;
				}else
					writeline("Question is the same");
			}
			//HELP command implementation
			if ((_com.startsWith("help") || _com.startsWith("man") || _com.startsWith("pony")) && depth == 0){
				writeline("clear : reset selection\n");
				writeline("remove n,n-n,n,n-n : remove some entries from selection");
				writeline("show n,n-n,n,n-n : show selected files and their paths");
				writeline("show : show selected files");
				writeline("add n,n-n,n,n-n : add files in n-positions and position ranges to selection");
				writeline("ls : list files in current directory");
				writeline("List of commands:");
				return;
			}
			//STH command implementation
			
			if (_com.equalsIgnoreCase("hey") && depth == 0){
				if (selected.size() == 0){
					writeline("E: No files selected. Use 'add' command to select files or 'help' to see manual");
					return;
				}
				depth = 7;
				Mp3File _hmm;
				try {
					_hmm = new Mp3File(selected.get(0));
					//writeline(_hmm.getId3v2Tag().getTitle());
					//writeline(_hmm.getId3v2Tag().getLyrics());
					//_hmm.getId3v2Tag().setLyrics("Let's\nfuck!");
					//_hmm.save(selected.get(0).getPath()+"hey");
				} catch (UnsupportedTagException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InvalidDataException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (NotSupportedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
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
				writeline("Some argument members were ignored 'cause of parsing error");
			}
		}
		return (_result);
	}
	
	private void writeline(String _pony){
		output.setText(_pony + "\n" + output.getText());
	}
}