package com.tutorialcodes;

import javax.sound.midi.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import java.io.*;
import java.net.*;
import javax.swing.filechooser.*;

public class BeatBoxPlayer
{
	static BeatBoxPlayer bbp;
	JFrame usernameBox;
	JTextField field;
	JFrame frame;
	JPanel mainPanel;
	JList incomingList;
	JTextArea userMessage;
	ArrayList<JCheckBox> checkBoxList;
	Vector<String> listVector = new Vector<String>();
	String userName;
	ObjectOutputStream out;
	ObjectInputStream in;
	HashMap<String, boolean[]> otherSeqsMap = new HashMap<String, boolean[]>();
	
	Sequencer sequencer;
	Sequence sequence;
	Track track;
	String[] instrumentNames = {"Bass Drum", "Closed Hi-Hat", "Open Hi-Hat",
	"Acoustic Snare", "Crash Cymbal", "Hand Clap", "High Tom", "Hi Bingo", 
	"Maracas", "Whistle", "Low Conga", "Cowbell", "Vibraslap", "Low-mid Tom",
	"High Agogo", "Open Hi Conga"};
	int[] instruments = {35, 42, 46, 38, 49, 39, 50, 60, 70, 72, 64, 56, 58, 47, 67, 63};
	
	public static void main(String[] args)
	{
		bbp = new BeatBoxPlayer();
		bbp.AskUsername();
	}
	 
	public void AskUsername()
	{
		usernameBox = new JFrame("Choose a username");
		JLabel label = new JLabel("Username(Cannot be empty): ");
		field = new JTextField();
		JButton ok = new JButton("OK");
		ok.addActionListener(new MyUsernameListener());
		usernameBox.add(BorderLayout.WEST, label);
		usernameBox.add(BorderLayout.CENTER, field);
		usernameBox.add(BorderLayout.EAST, ok);
		usernameBox.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		usernameBox.setBounds(100, 100, 500, 100);
		usernameBox.setVisible(true);
	}	
	 
	public void startUp(String name)
	{
		userName = name;
		// open connection to the server
		try {
			Socket sock = new Socket("127.0.0.1", 4242);
			out = new ObjectOutputStream(sock.getOutputStream());
			in = new ObjectInputStream(sock.getInputStream());
			Thread remote = new Thread(new RemoteReader());
			remote.start();
		} catch (Exception e) {
			System.out.println("couldn't connect - you'll have to play alone.");
		}
		setUpMidi();
		buildGUI();
	}
	
	public void buildGUI()
	{
		ArrayList<JButton> allButtons = new ArrayList<JButton>();
		
		frame = new JFrame("Cyber BeatBox");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		BorderLayout layout = new BorderLayout();
		JPanel background = new JPanel(layout);
		background.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		
		checkBoxList = new ArrayList<JCheckBox>();
		Box buttonBox = new Box(BoxLayout.Y_AXIS);
		
		JButton start = new JButton("Start");
		start.addActionListener(new MyStartListener());
		allButtons.add(start);

		JButton stop = new JButton("Stop");
		stop.addActionListener(new MyStopListener());
		allButtons.add(stop);

		JButton upTempo = new JButton("Tempo Up");
		upTempo.addActionListener(new MyUpTempoListener());
		allButtons.add(upTempo);

		JButton downTempo = new JButton("Tempo Down");
		downTempo.addActionListener(new MyDownTempoListener());
		allButtons.add(downTempo);
		
		JButton random = new JButton("Random");
		random.addActionListener(new MyRandomListener());
		allButtons.add(random);
		
		JButton save = new JButton("Save");
		save.addActionListener(new MySaveListener());
		allButtons.add(save);
		
		for (JButton button : allButtons)
		{
			button.setAlignmentX(Component.CENTER_ALIGNMENT);
			buttonBox.add(button);
			buttonBox.add(Box.createVerticalStrut(3));
		}
		
		JButton restore = new JButton("Select saved music");
		restore.addActionListener(new MyReadInListener());
		restore.setAlignmentX(Component.CENTER_ALIGNMENT);
		buttonBox.add(restore);
		buttonBox.add(Box.createVerticalStrut(6));
		
		incomingList = new JList();
		incomingList.addMouseListener(new MyListMouseListener());
		incomingList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		incomingList.setVisibleRowCount(12);
		JScrollPane theList = new JScrollPane(incomingList);
		buttonBox.add(theList);
		incomingList.setListData(listVector);
		buttonBox.add(Box.createVerticalStrut(3));
		
		userMessage = new JTextArea(2, 15);
		userMessage.setLineWrap(true);
		buttonBox.add(userMessage);
		buttonBox.add(Box.createVerticalStrut(3));
		
		JButton sendIt = new JButton("Send It");
		sendIt.addActionListener(new MySendListener());
		buttonBox.add(sendIt);
		buttonBox.add(Box.createVerticalStrut(6));

		Box nameBox = new Box(BoxLayout.Y_AXIS);
		for (int i = 0; i < 16; i++)
		{
			nameBox.add(new Label(instrumentNames[i]));
		}

		background.add(BorderLayout.EAST, buttonBox);
		background.add(BorderLayout.WEST, nameBox);
		frame.getContentPane().add(background);

		GridLayout grid = new GridLayout(16, 16);
		grid.setVgap(1);
		grid.setHgap(3);
		mainPanel = new JPanel(grid);
		background.add(BorderLayout.CENTER, mainPanel);

		for (int j = 0; j < 256; j++)
		{
			JCheckBox c = new JCheckBox();
			c.setSelected(false);
			checkBoxList.add(c);
			mainPanel.add(c);
		}

		frame.setBounds(50, 50, 300, 300);
		frame.pack();
		frame.setVisible(true);
	}

	public void setUpMidi()
	{
        try {
            sequencer = MidiSystem.getSequencer();
			sequencer.open();
			sequence = new Sequence(Sequence.PPQ, 4);
			track = sequence.createTrack();
			sequencer.setTempoInBPM(120);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void buildTrackAndStart()
	{ 
		ArrayList<Integer> trackList = null; 
		/* make a 16-element array to hold the values for one instrument, across all 16 beats. If the instrument is supposed to play on that beat, the value at that element will be the key. 
		If that instrument is NOT supposed to play on that beat, put in zero.*/
        
		sequence.deleteTrack(track); // get rid of the old track, make a fresh one.
		track = sequence.createTrack();

		for (int row = 0; row < 16; row++) // do this for each of the 16 rows (instruments)
		{
			trackList = new ArrayList<Integer>();
			for (int col = 0; col < 16; col++)
			{
                JCheckBox jc = (JCheckBox) checkBoxList.get(row + 16*col);
				if (jc.isSelected())
				{
					int key = instruments[row];
					trackList.add(new Integer(key));
				} 
				else
				{
					trackList.add(null);
				} 
			}
			makeTracks(trackList); // For this instrument, and for all 16 beats, make events and add them to the track
		}
		
		track.add(makeEvent(192, 9, 1, 0, 15)); // make sure that there is an event at beat 16 before it starts over in a new loop
	    try {
			sequencer.setSequence(sequence);
			sequencer.setLoopCount(sequencer.LOOP_CONTINUOUSLY);
			sequencer.start();
			sequencer.setTempoInBPM(120);
		} catch (Exception e) {
			e.printStackTrace();
		}		
	}

	public void makeTracks(ArrayList<Integer> list)
	{
		for (int k = 0; k < 16; k++)
		{
			Integer num = list.get(k);
			if (num != null)
			{
				int key = num.intValue();
				track.add(makeEvent(144, 9, key, 100, k));
				track.add(makeEvent(128, 9, key, 100, k+1));
			}
		}
	}

	public MidiEvent makeEvent(int comd, int channel, int one, int two, int tick)
	{
		MidiEvent event = null;
		try {
			ShortMessage a = new ShortMessage();
			a.setMessage(comd, channel, one, two);
			event = new MidiEvent(a, tick);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return event;
	}
	
	class MyUsernameListener implements ActionListener
	{
		public void actionPerformed(ActionEvent event)
		{
			String s = field.getText();
			if (s != null && !s.trim().equals(""))
			{
				bbp.startUp(s);
				usernameBox.dispose();
			}
		}
	}
	
	class MyStartListener implements ActionListener
	{
		public void actionPerformed(ActionEvent event)
		{
			buildTrackAndStart();
		}
	}
	
	class MyStopListener implements ActionListener
	{
		public void actionPerformed(ActionEvent event)
		{
			sequencer.stop();
		}
	}

	class MyUpTempoListener implements ActionListener
	{
		public void actionPerformed(ActionEvent event)
		{
            float tempoFactor = sequencer.getTempoFactor();
			sequencer.setTempoFactor((float)(tempoFactor * 1.05));
		}
	}

	class MyDownTempoListener implements ActionListener
	{
		public void actionPerformed(ActionEvent event)
		{
            float tempoFactor = sequencer.getTempoFactor();
			sequencer.setTempoFactor((float)(tempoFactor * 0.95));
		}
	}
	
	class MyRandomListener implements ActionListener
	{
		public void actionPerformed(ActionEvent event)
		{
			for (int v = 0; v < 256; v++)
            {
				double rand = Math.random();
				JCheckBox check = (JCheckBox)checkBoxList.get(v);
				if (rand <= 0.3d)
					check.setSelected(true);
				else 
					check.setSelected(false);
				sequencer.stop();
				buildTrackAndStart();
			}				
		}
	}
	
	class MySaveListener implements ActionListener 
	{
		public void actionPerformed(ActionEvent event)
		{
			JFileChooser fileSave = new JFileChooser();
			boolean[] checkBoxState = new boolean[256];
			for (int l = 0; l < 256; l++)
			{
				JCheckBox check = (JCheckBox)checkBoxList.get(l);
				if (check.isSelected())
					checkBoxState[l] = true;
			}
			FileNameExtensionFilter filter = new FileNameExtensionFilter("Cyber Beatbox Files (.music)", "music");
			fileSave.setFileFilter(filter);
			fileSave.setAcceptAllFileFilterUsed(false);
			int o = fileSave.showSaveDialog(null);
			File file = fileSave.getSelectedFile();
			if (o == JFileChooser.APPROVE_OPTION)
			{
				String fileName = file.toString();
				if (!fileName.endsWith(".music"))
                    file = new File(fileName + ".music");
			}
			try {
				FileOutputStream fileStream = new FileOutputStream(file);
				ObjectOutputStream os = new ObjectOutputStream(fileStream);
				os.writeObject(checkBoxState);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	class MyReadInListener implements ActionListener
	{
		JFileChooser fileOpen = new JFileChooser();
		public void actionPerformed(ActionEvent event)
		{
			FileNameExtensionFilter filter = new FileNameExtensionFilter("Cyber Beatbox Files (.music)", "music");
			fileOpen.setFileFilter(filter);
			fileOpen.setAcceptAllFileFilterUsed(false);
			fileOpen.showOpenDialog(null);
			boolean[] checkBoxState = null;
			try {
				FileInputStream fileIn = new FileInputStream(fileOpen.getSelectedFile());
				ObjectInputStream is = new ObjectInputStream(fileIn);
				checkBoxState = (boolean[])is.readObject();
			} catch (Exception e) {
				e.printStackTrace();
			}
			for (int m = 0; m < 256; m++)
			{
				JCheckBox check = (JCheckBox) checkBoxList.get(m);
				if (checkBoxState[m])
					check.setSelected(true);
				else
					check.setSelected(false);
			}
			sequencer.stop();
			buildTrackAndStart();
		}
	}
	
	class MySendListener implements ActionListener
	{
		public void actionPerformed(ActionEvent a)
		{
			// make and arraylist of just the STATE of the checkboxes
			boolean[] checkboxState = new boolean[256];
			for (int n = 0; n < 256; n++)
			{
				JCheckBox check = (JCheckBox) checkBoxList.get(n);
				if (check.isSelected())
				    checkboxState[n] = true;
			}
			String messageToSend = "";
			try {
				String s = userMessage.getText();
				if (s != null && !s.trim().equals(""))
					messageToSend = userName + ": " + userMessage.getText();
				else 
					messageToSend = userName + ": "+ "Check out my music!";
				out.writeObject(messageToSend);
				out.writeObject(checkboxState);
			} catch (Exception e) {
				System.out.println("Sorry dude. Could not send it to the server.");
			}
			userMessage.setText("");
		}
	}
	
	class MyListMouseListener implements MouseListener
	{
		public void mouseClicked(MouseEvent mse)
		{
				String selected = (String) incomingList.getSelectedValue();
				if (selected != null)
				{
					// now go to the map, and change the sequence
					JFrame dialog = new JFrame("Confirm play pattern");
					JLabel text = new JLabel("<html>Your current beat pattern will be lost if you play other people's beat pattern.<br/>Do you want to go back to save your pattern first?</html>");
					JButton play = new JButton("Don't worry, go ahead and play");
					play.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent a)
						{
							dialog.dispose();
							boolean[] selectedState = (boolean[]) otherSeqsMap.get(selected);
					        changeSequence(selectedState);
					        sequencer.stop();
					        buildTrackAndStart();
						}
					});
					JButton back = new JButton("Go back");
					back.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent a)
						{
							dialog.dispose();
						}
					});
					dialog.add(BorderLayout.NORTH, text);
					JPanel pan = new JPanel();
					pan.add(play);
					pan.add(back);
                    dialog.add(BorderLayout.CENTER, pan);
                    dialog.setBounds(100, 100, 350, 200);					
					dialog.setVisible(true);
				}
		}
		public void mouseEntered(MouseEvent mse) {}
		public void mouseReleased(MouseEvent mse) {}
		public void mousePressed(MouseEvent mse) {}
		public void mouseExited(MouseEvent mse) {}
	}
	
	class RemoteReader implements Runnable
	{
		boolean[] checkboxState = null;
		String nameToShow = null;
		Object obj = null;
		public void run()
		{
			try {
				while ((obj = in.readObject()) != null)
				{
					System.out.println("got an object from server");
					System.out.println(obj.getClass());
					String nameToShow = (String) obj;
					checkboxState = (boolean[]) in.readObject();
					otherSeqsMap.put(nameToShow, checkboxState);
					listVector.addElement(nameToShow);
					incomingList.setListData(listVector);
				}
			} catch (Exception e) {
					e.printStackTrace();
			}
		}
	}
	
	public void changeSequence(boolean[] checkboxState)
	{
		for (int x = 0; x < 256; x++)
		{
			JCheckBox check = (JCheckBox)checkBoxList.get(x);
			if (checkboxState[x])
				check.setSelected(true);
			else
				check.setSelected(false);
		}
	}
}