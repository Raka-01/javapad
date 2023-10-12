package com.raka.java.project.main;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.print.PrintService;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.BadLocationException;
import javax.swing.undo.UndoManager;

public class Javapad {

	private JFrame frame;
	JTabbedPane tabbedPane;
	JMenuBar menuBar;
	JMenu fileMenu, editMenu, viewMenu, viewZoom;
	JMenuItem fileNewTab, fileNewWindow, fileOpen, fileSave, fileSaveAs, fileSaveAll, filePageSetup, filePrint, fileCloseTab, fileCloseWindow, fileExit;
	JMenuItem editUndo, editRedo, editCut, editCopy, editPaste, editDelete, editFind, editFindNext, editFindPrevious, editReplace, editGoTo, editSelectAll, editTimeDate, editFont;
	JMenuItem viewZoomIn, viewZoomOut, viewZoomRestore;
	JCheckBoxMenuItem viewStatusBar, viewWordWrap;
	JPanel statusBar;
	JLabel barCaretRecord, barZoom;
	
	UndoManager undoManager;
	
	int tabIndex = -1;
	String fileName = "Untitled-";
	static int currentFontSize = 12;
	static int currentZoom = 100;
	static String zoomPercent = currentZoom + "%";
	static int MAX_ZOOM = 250;
	static int MIN_ZOOM = 50;
	String lineEnding = "N/A";
	
	
	Map<Component, File> tabFiles = new HashMap<Component, File>();
	Map<Component, Boolean> tabSaved = new HashMap<Component, Boolean>();
	
	JFileChooser fileChooser;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					Javapad window = new Javapad();
					window.frame.setVisible(true);
				} catch (Exception e) {
					JOptionPane.showMessageDialog(null, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE, null);
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public Javapad() {
		constructWindow();
		
		tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		frame.getContentPane().add(tabbedPane, BorderLayout.CENTER);
		tabbedPane.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				int index = tabbedPane.getSelectedIndex();
				JTextArea textArea = getSelectedTextArea(index);
				updateCaretPosition(textArea, tabbedPane);
				
			}
		});
		
		fileChooser = new JFileChooser();
		fileChooser.setCurrentDirectory(new File("."));
		
		constructMenuBar(tabbedPane);
		initializeTab(tabbedPane);
		
	}

	protected JTextArea getSelectedTextArea(int index) {
		Component selectedTab = tabbedPane.getComponentAt(index);
		if(selectedTab instanceof JScrollPane) {
			JScrollPane pane = (JScrollPane)selectedTab;
			JTextArea textArea = (JTextArea)pane.getViewport().getView();
			return textArea;
		}
		return null;
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void constructWindow() {
		frame = new JFrame("Javapad");
		frame.setBounds(100, 100, 800, 500);
		
		ImageIcon icon = new ImageIcon("./resources/notepad-icon-png-5.jpg");
		frame.setIconImage(icon.getImage());
		
		
		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				closeWindow(tabbedPane);
			}
		});
		
		
		JPanel leftBorderPanel = new JPanel();
		frame.getContentPane().add(leftBorderPanel, BorderLayout.WEST);
		
		JLabel leftBorder = new JLabel("");
		leftBorderPanel.add(leftBorder);
		
		JPanel rightBorderPanel = new JPanel();
		frame.getContentPane().add(rightBorderPanel, BorderLayout.EAST);
		
		JLabel rightBorder = new JLabel("");
		rightBorderPanel.add(rightBorder);
		
		statusBar = new JPanel();
		frame.getContentPane().add(statusBar, BorderLayout.SOUTH);
		barCaretRecord = new JLabel();
		
	}
	
	/*
	 * Instantiate Tab and its objects & fields
	 */
	private void initializeTab(JTabbedPane tabbedPane) {
		openNewTab(tabbedPane);
		setStatusBar();
	}
	
	private void setStatusBar() {
		
		barZoom = new JLabel();
		barZoom.setText(zoomPercent);
		barZoom.setAlignmentX(Component.RIGHT_ALIGNMENT);
		barZoom.setHorizontalAlignment(SwingConstants.RIGHT);
		
		JLabel barWindowsFormat = new JLabel("Windows (CRLF)");
		barWindowsFormat.setToolTipText("Feature Not Immplemented");
		barWindowsFormat.setHorizontalAlignment(SwingConstants.RIGHT);
		barWindowsFormat.setAlignmentX(Component.RIGHT_ALIGNMENT);
		
		JLabel barCharEncoding = new JLabel("UTF-8");
		barCharEncoding.setToolTipText("Feature Not Implemented");
		barCharEncoding.setAlignmentX(Component.RIGHT_ALIGNMENT);
		barCharEncoding.setHorizontalAlignment(SwingConstants.RIGHT);
		GroupLayout gl_statusBar = new GroupLayout(statusBar);
		gl_statusBar.setHorizontalGroup(
			gl_statusBar.createParallelGroup(Alignment.TRAILING)
				.addGroup(Alignment.LEADING, gl_statusBar.createSequentialGroup()
					.addGap(30)
					.addComponent(barCaretRecord, GroupLayout.PREFERRED_SIZE, 110, GroupLayout.PREFERRED_SIZE)
					.addGap(198)
					.addComponent(barZoom, GroupLayout.DEFAULT_SIZE, 58, Short.MAX_VALUE)
					.addGap(60)
					.addComponent(barWindowsFormat)
					.addGap(60)
					.addComponent(barCharEncoding)
					.addGap(124))
		);
		gl_statusBar.setVerticalGroup(
			gl_statusBar.createParallelGroup(Alignment.LEADING)
				.addComponent(barWindowsFormat, Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, 18, Short.MAX_VALUE)
				.addComponent(barCharEncoding, Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, 18, Short.MAX_VALUE)
				.addComponent(barZoom, Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, 18, Short.MAX_VALUE)
				.addComponent(barCaretRecord, Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, 18, Short.MAX_VALUE)
		);
		statusBar.setLayout(gl_statusBar);
		
	}

	private void constructMenuBar(JTabbedPane tabbedPane) {
		menuBar = new JMenuBar();
		frame.setJMenuBar(menuBar);
		
		fileMenu = new JMenu("File");
		menuBar.add(fileMenu);
		
		editMenu = new JMenu("Edit");
		editMenu.addMenuListener(new MenuListener() {
			
			@Override
			public void menuSelected(MenuEvent e) {
				JTextArea textArea = getSelectedTextArea(tabbedPane.getSelectedIndex());
				
				if(textArea.getText().length() == 0) {
					editFind.setEnabled(false);
					editFindNext.setEnabled(false);
					editFindPrevious.setEnabled(false);
					editReplace.setEnabled(false);
				}
				else {
					editFind.setEnabled(true);
					editFindNext.setEnabled(true);
					editFindPrevious.setEnabled(true);
					editReplace.setEnabled(true);
				}
				
				if(textArea.getSelectionStart() == textArea.getSelectionEnd()) {
					editCut.setEnabled(false);
					editCopy.setEnabled(false);
					editDelete.setEnabled(false);
				}
				else {
					editCut.setEnabled(true);
					editCopy.setEnabled(true);
					editDelete.setEnabled(true);
				}
				
				if(undoManager.canUndo()) {
					editUndo.setEnabled(true);
				}
				else {
					editUndo.setEnabled(false);
				}
				
				if(undoManager.canRedo()) {
					editRedo.setEnabled(true);
				}
				else {
					editRedo.setEnabled(false);
				}
				
				Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
				Transferable transferable = clipboard.getContents(this);
				if(transferable == null) {
					editPaste.setEnabled(false);
				}
				else {
					editPaste.setEnabled(true);
				}
			}
			
			@Override
			public void menuDeselected(MenuEvent e) {}
			
			@Override
			public void menuCanceled(MenuEvent e) {}
		});
		menuBar.add(editMenu);
		
		viewMenu = new JMenu("View");
		menuBar.add(viewMenu);
		
		
		
		fileNewTab = new JMenuItem("New tab");
		fileNewTab.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.CTRL_DOWN_MASK));
		fileNewTab.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				openNewTab(tabbedPane);
				
			}
		});
		fileMenu.add(fileNewTab);
		
		fileNewWindow = new JMenuItem("New window");
		fileNewWindow.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.SHIFT_DOWN_MASK | KeyEvent.CTRL_DOWN_MASK));
		fileNewWindow.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				openNewWindow();
				
			}
		});
		fileMenu.add(fileNewWindow);
		
		fileOpen = new JMenuItem("Open");
		fileOpen.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.CTRL_DOWN_MASK));
		fileOpen.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				openFile(tabbedPane);
				
			}
		});
		fileMenu.add(fileOpen);
		
		fileSave = new JMenuItem("Save");
		fileSave.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK));
		fileSave.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				saveFile(tabbedPane);
				
			}
		});
		fileMenu.add(fileSave);
		
		fileSaveAs = new JMenuItem("Save as");
		fileSaveAs.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK));
		fileSaveAs.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				saveAsFile(tabbedPane);
				
			}
		});
		fileMenu.add(fileSaveAs);
		
		fileSaveAll = new JMenuItem("Save all");
		fileSaveAll.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK | KeyEvent.ALT_DOWN_MASK));
		fileSaveAll.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				saveAllFile(tabbedPane);
				
			}
		});
		fileMenu.add(fileSaveAll);
		
		JSeparator fileSeparator1 = new JSeparator();
		fileMenu.add(fileSeparator1);
		
		filePageSetup = new JMenuItem("Page setup");
		filePageSetup.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JOptionPane.showMessageDialog(frame, "Method Not Implemented. Coming Soon...");
				
			}
		});
		fileMenu.add(filePageSetup);
		
		filePrint = new JMenuItem("Print");
		filePrint.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, KeyEvent.CTRL_DOWN_MASK));
		filePrint.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				printFile(tabbedPane);
				
			}
		});
		fileMenu.add(filePrint);
		
		JSeparator fileSeparator2 = new JSeparator();
		fileMenu.add(fileSeparator2);
		
		fileCloseTab = new JMenuItem("Close tab");
		fileCloseTab.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, KeyEvent.CTRL_DOWN_MASK));
		fileCloseTab.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				closeCurrentTab(tabbedPane);
				
			}
		});
		fileMenu.add(fileCloseTab);
		
		fileCloseWindow = new JMenuItem("Close window");
		fileCloseWindow.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK));
		fileCloseWindow.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				closeWindow(tabbedPane);
				
			}
		});
		fileMenu.add(fileCloseWindow);
		
		fileExit = new JMenuItem("Exit");
		fileExit.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				System.exit(0);
				
			}
		});
		fileMenu.add(fileExit);
		
		
		
		editUndo = new JMenuItem("Undo");
		editUndo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, KeyEvent.CTRL_DOWN_MASK));
		editUndo.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				undo(tabbedPane);
			}
		});
		editMenu.add(editUndo);
		
		editRedo = new JMenuItem("Redo");
		editRedo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Y, KeyEvent.CTRL_DOWN_MASK));
		editRedo.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				redo(tabbedPane);
			}
		});
		editMenu.add(editRedo);
		
		JSeparator editSeparator1 = new JSeparator();
		editMenu.add(editSeparator1);
		
		editCut = new JMenuItem("Cut");
		editCut.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, KeyEvent.CTRL_DOWN_MASK));
		editCut.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				cutText(tabbedPane);
			}
		});
		editMenu.add(editCut);
		
		editCopy = new JMenuItem("Copy");
		editCopy.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.CTRL_DOWN_MASK));
		editCopy.addActionListener(new ActionListener() {	
			@Override
			public void actionPerformed(ActionEvent e) {
				copyText(tabbedPane);
				
			}
		});
		editMenu.add(editCopy);
		
		editPaste = new JMenuItem("Paste");
		editPaste.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, KeyEvent.CTRL_DOWN_MASK));
		editPaste.addActionListener(new ActionListener() {	
			@Override
			public void actionPerformed(ActionEvent e) {
				pasteText(tabbedPane);
				
			}
		});
		editMenu.add(editPaste);
		
		editDelete = new JMenuItem("Delete");
		editDelete.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));
		editDelete.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				deleteText(tabbedPane);
			}
		});
		editMenu.add(editDelete);
		
		JSeparator editSeparator2 = new JSeparator();
		editMenu.add(editSeparator2);
		
		editFind = new JMenuItem("Find");
		editFind.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.CTRL_DOWN_MASK));
		editFind.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				findText(tabbedPane);
			}
		});
		editMenu.add(editFind);
		
		editFindNext = new JMenuItem("Find next");
		editFindNext.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0));
		editFindNext.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JOptionPane.showMessageDialog(frame, "Method Not Implemented. Coming Soon...");			}
		});
		editMenu.add(editFindNext);
		
		editFindPrevious = new JMenuItem("Find previous");
		editFindPrevious.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F3, KeyEvent.SHIFT_DOWN_MASK));
		editFindPrevious.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JOptionPane.showMessageDialog(frame, "Method Not Implemented. Coming Soon...");
			}
		});
		editMenu.add(editFindPrevious);
		
		editReplace = new JMenuItem("Replace");
		editReplace.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_H, KeyEvent.CTRL_DOWN_MASK));
		editReplace.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JOptionPane.showMessageDialog(frame, "Method Not Implemented. Coming Soon...");
				
			}
		});
		editMenu.add(editReplace);
		
		editGoTo = new JMenuItem("Go to");
		editGoTo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_G, KeyEvent.CTRL_DOWN_MASK));
		editGoTo.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				goToLine(tabbedPane);
			}
		});
		editMenu.add(editGoTo);
		
		JSeparator editSeparator3 = new JSeparator();
		editMenu.add(editSeparator3);
		
		editSelectAll = new JMenuItem("Select all");
		editSelectAll.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, KeyEvent.CTRL_DOWN_MASK));
		editSelectAll.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				selectAllText(tabbedPane);
				
			}
		});
		editMenu.add(editSelectAll);
		
		editTimeDate = new JMenuItem("Time/Date");
		editTimeDate.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));
		editTimeDate.addActionListener(new ActionListener() {	
			@Override
			public void actionPerformed(ActionEvent e) {
				insertTimeDate(tabbedPane);
				
			}
		});
		editMenu.add(editTimeDate);
		
		JSeparator editSeparator4 = new JSeparator();
		editMenu.add(editSeparator4);
		
		editFont = new JMenuItem("Font");
		editFont.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JOptionPane.showMessageDialog(frame, "Method Not Implemented. Coming Soon...");
				
			}
		});
		editMenu.add(editFont);
		
		
		
		viewZoom = new JMenu("Zoom");
		viewMenu.add(viewZoom);
		
		viewZoomIn = new JMenuItem("Zoom in");
		viewZoomIn.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_ADD, KeyEvent.CTRL_DOWN_MASK));
		viewZoomIn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				zoomInText(tabbedPane);
				
			}
		});
		viewZoom.add(viewZoomIn);
		
		viewZoomOut = new JMenuItem("Zoom out");
		viewZoomOut.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT, KeyEvent.CTRL_DOWN_MASK));
		viewZoomOut.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				zoomOutText(tabbedPane);
				
			}
		});
		viewZoom.add(viewZoomOut);
		
		viewZoomRestore = new JMenuItem("Restore default zoom");
		viewZoomRestore.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD0, KeyEvent.CTRL_DOWN_MASK));
		viewZoomRestore.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				restoreDefaultZoom(tabbedPane);
				
			}
		});
		viewZoom.add(viewZoomRestore);
		
		viewStatusBar = new JCheckBoxMenuItem("Status bar");
		viewStatusBar.setSelected(true);
		viewStatusBar.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if(viewStatusBar.isSelected()) {
					statusBar.setVisible(true);
				}
				else {
					statusBar.setVisible(false);
				}
			}
		});
		viewMenu.add(viewStatusBar);
		viewWordWrap = new JCheckBoxMenuItem("Word wrap");
		viewWordWrap.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				for(int i = 0; i < tabbedPane.getTabCount(); i++) {
					JTextArea textArea = getSelectedTextArea(i);
					wordWrap(textArea);
				}
				
			}
		});
		viewMenu.add(viewWordWrap);
	}

	protected void wordWrap(JTextArea textArea) {
		if(viewWordWrap.isSelected()) {
			textArea.setLineWrap(true);
			textArea.setWrapStyleWord(true);
		}
		else {
			textArea.setLineWrap(false);
			textArea.setWrapStyleWord(false);
		}
		
	}

	protected void openNewTab(JTabbedPane tabbedPane) {
		EventQueue.invokeLater(() -> {					// new Runnable thread is implemented - lambda expression
			tabIndex = tabbedPane.getTabCount();
			
			JTextArea textArea = new JTextArea();
			JScrollPane pane = new JScrollPane(textArea);
			tabbedPane.addTab((fileName+(tabIndex+1)), null, pane, null);
			tabbedPane.setSelectedIndex(tabIndex);
			textArea.setCaretPosition(0);
			textArea.setFont(new Font("Arial", 0, currentFontSize));
			textArea.requestFocusInWindow();
			wordWrap(textArea);
			
			textArea.addCaretListener(e -> updateCaretPosition(textArea, tabbedPane));
			
			tabSaved.put(pane, false);
			
			undoManager = new UndoManager();
			textArea.getDocument().addUndoableEditListener(new UndoableEditListener() {
				
				@Override
				public void undoableEditHappened(UndoableEditEvent e) {
					undoManager.addEdit(e.getEdit());
					
				}
			});
				
		});
		
	}
	
	private void updateCaretPosition(JTextArea textArea, JTabbedPane tabbedPane) {
		int line = 0;
		int column = 0;
		int position = 0;
		
		try {
			position = textArea.getCaretPosition();
			line = textArea.getLineOfOffset(position);
			column = position - textArea.getLineStartOffset(line);
		}
		catch(BadLocationException e) {
			JOptionPane.showMessageDialog(null, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE, null);
		}
		
		if(textArea.getText().length() == 0) {
			line = 0;
			column = 0;
		}
		
		barCaretRecord.setText("   Ln " + (line+1) + ", Col " + (column+1));
		
	}

	protected void openNewWindow() {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					Javapad window = new Javapad();
					window.frame.setVisible(true);
				} catch (Exception e) {
					JOptionPane.showMessageDialog(null, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE, null);
				}
			}
		});
		
	}
	
	protected void openFile(JTabbedPane tabbedPane) {
		int command = fileChooser.showOpenDialog(frame);
		
		if(command == JFileChooser.APPROVE_OPTION) {
			File file = fileChooser.getSelectedFile();
			
			if(!tabFiles.containsValue(file)) {
				try(BufferedReader reader = new BufferedReader(new FileReader(file));) {
					StringBuilder fileContent = new StringBuilder();
					String line;
					while((line = reader.readLine()) != null) {
						fileContent.append(line).append("\n");
					}
					
					String fileName = file.getName();
					
					JTextArea textArea = new JTextArea();
					textArea.setFont(new Font("Arial", 0, currentFontSize));
					textArea.setText(fileContent.toString());
					JScrollPane pane = new JScrollPane(textArea);
					tabbedPane.addTab(fileName, null, pane, null);
					tabIndex = tabbedPane.getTabCount() - 1;
					tabbedPane.setSelectedIndex(tabIndex);
					
					textArea.requestFocusInWindow();
					
					wordWrap(textArea);
					
					Component selectedTab = tabbedPane.getSelectedComponent();
					tabFiles.put(selectedTab, file);
					tabSaved.put(selectedTab, false);
					
					
					textArea.addCaretListener(e -> updateCaretPosition(textArea, tabbedPane));
				}
				catch(IOException e) {
					JOptionPane.showMessageDialog(null, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE, null);
				}
			}
			else {
				for(Map.Entry<Component, File> entry : tabFiles.entrySet()) {
					if(entry.getValue().equals(file)) {
						tabbedPane.setSelectedComponent(entry.getKey());
						break;
					}
				}
			}
		}
	}
	
	protected int saveFile(JTabbedPane tabbedPane) {
		int fileSaved = 1;
		int index = tabbedPane.getSelectedIndex();
		if(index != -1) {
			Component selectedTab = tabbedPane.getComponentAt(index);
			JTextArea textArea = getSelectedTextArea(index);
			File tabFile = tabFiles.get(selectedTab);
			
			if(tabFile != null) {
				if(writeData(tabFile, textArea)) {
					tabSaved.put(tabbedPane.getSelectedComponent(), true);
					fileSaved = 0;
				}
				
			}
			else {
				int command = fileChooser.showSaveDialog(frame);
				if(command == JFileChooser.APPROVE_OPTION) {
					File file = fileChooser.getSelectedFile();
					
					if(writeData(file, textArea)) {
						tabbedPane.setTitleAt(index, file.getName());
						tabFiles.put(selectedTab, file);
						tabSaved.put(tabbedPane.getSelectedComponent(), true);
						fileSaved = JFileChooser.APPROVE_OPTION;
					}
					
				}
			}
		}
		return fileSaved;
	}

	protected void saveAsFile(JTabbedPane tabbedPane) {
		int index = tabbedPane.getSelectedIndex();
		if(index != -1) {
			Component selectedTab = tabbedPane.getSelectedComponent();
			JTextArea textArea = getSelectedTextArea(index);
			
			int command = fileChooser.showSaveDialog(frame);
			if(command == JFileChooser.APPROVE_OPTION) {
				File file = fileChooser.getSelectedFile();
				
				if(writeData(file, textArea)) {
					tabbedPane.setTitleAt(index, file.getName());
					tabFiles.put(selectedTab, file);
					tabSaved.put(tabbedPane.getSelectedComponent(), true);
				}
					
			}
		}
	}

	protected void saveAllFile(JTabbedPane tabbedPane) {
		int tabCount = tabbedPane.getTabCount();
		
		for(int i = 0; i < tabCount; i++){
			tabbedPane.setSelectedIndex(i);
			Component selectedTab = tabbedPane.getComponentAt(i);
			JTextArea textArea = getSelectedTextArea(i);
			
			File tabFile = tabFiles.get(selectedTab);
			
			if(tabFile != null) {		// a file in directory is associated with the opened file in Javapad 
				if(writeData(tabFile, textArea)) {
					tabSaved.put(tabbedPane.getSelectedComponent(), true);
				}
				
			}
			else {
				int command = fileChooser.showSaveDialog(frame);
				if(command == JFileChooser.APPROVE_OPTION) {
					File file = fileChooser.getSelectedFile();
					
					if(writeData(file, textArea)) {
						tabbedPane.setTitleAt(tabbedPane.getSelectedIndex(), file.getName());
						tabFiles.put(selectedTab, file);
						tabSaved.put(tabbedPane.getSelectedComponent(), true);
					}
					
				}
				else if(command == JFileChooser.CANCEL_OPTION) {
					break;
				}
			}
		}
	}
	
	protected boolean writeData(File file, JTextArea textArea) {
		try {
			FileWriter fileWriter = new FileWriter(file);
			BufferedWriter writer = new BufferedWriter(fileWriter);
			String text = textArea.getText();
			
			writer.write(text);
			writer.close();
			
			return true;
		} catch (IOException e) {
			JOptionPane.showMessageDialog(null, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE, null);
			return false;
		}
	}
	
	// Due to system default settings this method is saving the the file as pdf in my pc
	protected void printFile(JTabbedPane tabbedPane) {
		PrintService[] printServices = PrinterJob.lookupPrintServices();
		
		if(printServices.length == 0) {
			JOptionPane.showMessageDialog(null, "No printer is available");
		}
		else {
			Printable printable = new Printable() {
				
				@Override
				public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
					if(pageIndex > 0) {
						return NO_SUCH_PAGE;
					}
					Graphics2D g2d = (Graphics2D)graphics;
					g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
					String text = "";
					
					int index = tabbedPane.getSelectedIndex();
					text = getSelectedTextArea(index).getText();
					
					g2d.drawString(text, 100, 100);
					return PAGE_EXISTS;
				}
			};
			
			PrinterJob printerJob = PrinterJob.getPrinterJob();
//			try {
//				printerJob.setPrintService(null);
//			} catch (PrinterException e) {
//				e.printStackTrace();
//			}
			if(printerJob.printDialog()) {
				printerJob.setPrintable(printable);
				try {
					printerJob.print();
				
				} catch (PrinterException e) {
					JOptionPane.showMessageDialog(null, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE, null);
				}
			}
		}
	}
	
	protected boolean closeCurrentTab(JTabbedPane tabbedPane) {
		boolean tabClosed = false;
		int index = tabbedPane.getSelectedIndex();
		int tabCount = tabbedPane.getTabCount();
		boolean isTabSaved = tabSaved.get(tabbedPane.getComponentAt(index));
		if(tabCount == 1) {
			if(isTabSaved) {
				frame.dispose();
			}
			else {
				int command = JOptionPane.showConfirmDialog(frame, "Do you want to save the file?");
				if(command == JOptionPane.YES_OPTION) {
					int fileSaved = saveFile(tabbedPane);
					if(fileSaved == JFileChooser.APPROVE_OPTION) {
						frame.dispose();
					}
				}
				else if(command == JOptionPane.NO_OPTION) {
					frame.dispose();
				}
			}
		}
		else {
			if(isTabSaved) {
				tabFiles.remove(tabbedPane.getComponentAt(index));
				tabbedPane.remove(index);
				tabClosed = true;
			}
			else {
				int command = JOptionPane.showConfirmDialog(frame, "Do you want to save the file?");
				if(command == JOptionPane.YES_OPTION) {
					int fileSaved = saveFile(tabbedPane);
					if(fileSaved == JFileChooser.APPROVE_OPTION) {
						tabbedPane.remove(index);
						tabClosed = true;
					}
				}
				else if(command == JOptionPane.NO_OPTION) {
					tabbedPane.remove(index);
					tabClosed = true;
				}
			}
		}
		return tabClosed;
	}
	
	protected void closeWindow(JTabbedPane tabbedPane) {
		int tabCount = tabbedPane.getTabCount();
		
		while(tabCount > 0) {
			boolean tabClosed = closeCurrentTab(tabbedPane);
			if(!tabClosed) {
				break;
			}
		}
		
	}
	
	
	
	protected void undo(JTabbedPane tabbedPane) {
		if(undoManager.canUndo()) {
			undoManager.undo();
		}
		
	}
	
	protected void redo(JTabbedPane tabbedPane) {
		if(undoManager.canRedo()) {
			undoManager.redo();
		}
	}
	
	protected void cutText(JTabbedPane tabbedPane2) {
		JTextArea textArea = getSelectedTextArea(tabbedPane.getSelectedIndex());
		String selectedText = textArea.getSelectedText();
		
		if(selectedText != null) {
			int start = textArea.getSelectionStart();
			int end = textArea.getSelectionEnd();
			textArea.replaceRange("", start, end);
			StringSelection selection = new StringSelection(selectedText);
			Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			clipboard.setContents(selection, null);
		}
		
		
	}

	protected void copyText(JTabbedPane tabbedPane2) {
		JTextArea textArea = getSelectedTextArea(tabbedPane.getSelectedIndex());
		String selectedText = textArea.getSelectedText();
		
		if(selectedText != null) {
			StringSelection selection = new StringSelection(selectedText);
			Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			clipboard.setContents(selection, null);
		}
		
	}
	
	protected void pasteText(JTabbedPane tabbedPane) {
		JTextArea textArea = getSelectedTextArea(tabbedPane.getSelectedIndex());
		
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		Transferable transferable = clipboard.getContents(this);
		
		if(transferable != null && transferable.isDataFlavorSupported(DataFlavor.stringFlavor)) {
			try {
				String pastedText = (String)transferable.getTransferData(DataFlavor.stringFlavor);
				int caretPosition = textArea.getCaretPosition();
				textArea.insert(pastedText, caretPosition);
			} catch (UnsupportedFlavorException | IOException e) {
				JOptionPane.showMessageDialog(null, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE, null);
			}
		}
	}
	
	protected void deleteText(JTabbedPane tabbedPane) {
		JTextArea textArea = getSelectedTextArea(tabbedPane.getSelectedIndex());
		String selectedText = textArea.getSelectedText();
		
		if(selectedText != null) {
			int start = textArea.getSelectionStart();
			int end = textArea.getSelectionEnd();
			textArea.replaceRange("", start, end);
		}
	}
	
	protected void findText(JTabbedPane tabbedPane2) {
		String searchText = JOptionPane.showInputDialog("Enter your text to search - ");
		JTextArea textArea = getSelectedTextArea(tabbedPane.getSelectedIndex());
		
		if(searchText != null) {
			String text = textArea.getText();
			int index = text.indexOf(searchText);
			
			while(index != -1) {
				textArea.select(index, index + searchText.length());
				index = text.indexOf(searchText, index + searchText.length());
			}
			
			if(textArea.getSelectionStart() == textArea.getSelectionEnd()) {
				JOptionPane.showMessageDialog(null, "No more occurences found");
				textArea.setSelectionStart(0);
				textArea.setSelectionEnd(0);
			}
		}
		
	}

	protected void goToLine(JTabbedPane tabbedPane) {
		try {
			int lineNo = Integer.parseInt(JOptionPane.showInputDialog("Enter Line Number - "));
			JTextArea textArea = getSelectedTextArea(tabbedPane.getSelectedIndex());
			
			int totalLines = textArea.getLineCount();
			
			if(lineNo > 0) {
				if(lineNo <= totalLines) {
					int offset = textArea.getLineStartOffset(lineNo - 1);
					textArea.setCaretPosition(offset);
				}
				else {
					JOptionPane.showMessageDialog(null, "Line number exceeded total lines");
					goToLine(tabbedPane);
				}
			}
			else {
				JOptionPane.showMessageDialog(null, "Invalid Line number");
				goToLine(tabbedPane);
			}
		}
		catch(NumberFormatException e) {
			JOptionPane.showMessageDialog(null, "Invalid Input, Please Enter a valid line number");
			goToLine(tabbedPane);
		}
		catch(BadLocationException e) {
			JOptionPane.showMessageDialog(null, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE, null);
		}
		
	}
	
	protected void selectAllText(JTabbedPane tabbedPane) {
		JTextArea textArea = getSelectedTextArea(tabbedPane.getSelectedIndex());
		int start = 0;
		int end = textArea.getText().length();
		
		textArea.select(start, end);
	}
	
	protected void insertTimeDate(JTabbedPane tabbedPane) {
		SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
		String formattedDate = dateFormat.format(new Date());
		
		JTextArea textArea = getSelectedTextArea(tabbedPane.getSelectedIndex());
		int caretPosition = textArea.getCaretPosition();
		
		textArea.insert(formattedDate, caretPosition);
	}
	
	protected void zoomInText(JTabbedPane tabbedPane) {
		int tabCount = tabbedPane.getTabCount();
		currentFontSize += 2;
		currentZoom += 10;
		if(currentZoom < MAX_ZOOM) {
			for(int i = 0; i < tabCount; i++) {
				JTextArea textArea = getSelectedTextArea(i);
				textArea.setFont(new Font("Arial", 0, currentFontSize));
				zoomPercent = currentZoom + "%";
				barZoom.setText(zoomPercent);
			}
		}
	}
	
	protected void zoomOutText(JTabbedPane tabbedPane) {
		int tabCount = tabbedPane.getTabCount();
		currentFontSize -= 2;
		currentZoom -=10;
		if(currentZoom > MIN_ZOOM) {
			for(int i = 0; i < tabCount; i++) {
				JTextArea textArea = getSelectedTextArea(i);
				textArea.setFont(new Font("Arial", 0, currentFontSize));
				zoomPercent = currentZoom + "%";
				barZoom.setText(zoomPercent);
			}
		}
	}
	
	protected void restoreDefaultZoom(JTabbedPane tabbedPane) {
		currentFontSize = 12;
		currentZoom = 100;
		zoomPercent = currentZoom + "%";
		for(int i = 0; i < tabbedPane.getTabCount(); i++) {
			JTextArea textArea = getSelectedTextArea(i);
			textArea.setFont(new Font("Arial", 0, currentFontSize));
			barZoom.setText(zoomPercent);
		}
	}

}
