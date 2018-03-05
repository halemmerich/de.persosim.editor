package de.persosim.editor.ui.editor.handlers;

import java.awt.GridLayout;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.TreeItem;

import de.persosim.editor.ui.editor.checker.FieldCheckResult;
import de.persosim.editor.ui.editor.checker.TextFieldChecker;
import de.persosim.simulator.utils.HexString;

public class EditorFieldHelper {
	
	public static void createBinaryField(TreeItem item, boolean mandatory, Composite composite, TlvModifier modifier, TextFieldChecker checker, String infoText){
		
		Text field = createField(item, mandatory, composite, modifier, checker, infoText);
		
		//dummy to fill empty cell
		new Label(composite, SWT.NONE);
		
		Composite buttons = new Composite(composite, SWT.NONE);
		buttons.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		buttons.setLayout(new RowLayout());
		
		Button replace = new Button(buttons, SWT.PUSH);
		replace.setText("Browse for Replacement");
		replace.addSelectionListener(new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				FileDialog fd = new FileDialog(Display.getDefault().getActiveShell(), SWT.OPEN);
				fd.setText("Open");
				fd.setFilterPath("C:/");
				String[] filterExt = { "*.bin", "*.*" };
				fd.setFilterExtensions(filterExt);
				String selection = fd.open();
				if (selection != null) {
					try {
						modifier.setValue(HexString.encode(Files.readAllBytes(Paths.get(selection))));
						ObjectHandler handler = (ObjectHandler) item.getData(ObjectHandler.HANDLER);
						if (handler != null) {
							handler.updateTextRecursively(item);
						}
						field.setText(modifier.getValue());
						for (Control control : composite.getChildren()){
							control.notifyListeners(SWT.Modify, new Event());
						}
					} catch (IOException e1) {
						MessageDialog.openError(Display.getCurrent().getActiveShell(), "Error", "File could not be read.");
					}
				}
			}
		});
		
		Button save = new Button(buttons, SWT.PUSH);
		save.setText("Save data");
		save.addSelectionListener(new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				FileDialog fd = new FileDialog(Display.getDefault().getActiveShell(), SWT.SAVE);
				fd.setText("Save");
				fd.setFilterPath("C:/");
				String[] filterExt = { "*.bin", "*.*" };
				fd.setFilterExtensions(filterExt);
				String selection = fd.open();
				if (selection != null) {
					try {
						Files.write(Paths.get(selection), HexString.toByteArray(modifier.getValue()));
					} catch (IOException e1) {
						MessageDialog.openError(Display.getCurrent().getActiveShell(), "Error", "File could not be written.");
					}
				}
			}
		});
		composite.pack();
	}
	
	public static Text createField(TreeItem item, boolean mandatory, Composite composite, TlvModifier modifier, TextFieldChecker checker, String infoText) {
		return createField(item, mandatory, true, composite, modifier, checker, infoText);
	}
	
	/**
	 * @param item
	 * @param mandatory
	 * @param composite Expected to have a {@link GridLayout} with 2 columns
	 * @param modifier
	 * @param charset
	 * @param infoText
	 */
	public static Text createField(TreeItem item, boolean mandatory, boolean editable, Composite composite, TlvModifier modifier, TextFieldChecker checker, String infoText) {
		Label info = new Label(composite, SWT.NONE);
		info.setText(infoText);
		GridData gd = new GridData();
		gd.horizontalSpan = 2;
		info.setLayoutData(gd);

		Button fieldUsed = null;
		
		if (!mandatory){
			fieldUsed = new Button(composite, SWT.CHECK);
		}

		Text field = new Text(composite, SWT.BORDER);
		GridData layoutData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		
		if (mandatory){
			layoutData.horizontalSpan = 2;
		}
		
		field.setLayoutData(layoutData);
		
		Color defaultColor = field.getBackground();


		Label warning = new Label(composite, SWT.NONE);
		warning.setText(infoText);
		gd = new GridData();
		gd.horizontalSpan = 2;
		warning.setLayoutData(gd);
		warning.setText("");

		String value = modifier.getValue();
		
		if (value != null){
			field.setText(value);
		}
		
		if (editable && (mandatory || value != null)) {
			if (fieldUsed != null){
				fieldUsed.setSelection(true);	
			}
			field.setEnabled(true);
			checkAndModify(field, modifier, checker, defaultColor, warning);
		} else {
			field.setEnabled(false);
		}

		if (editable) {
			field.addModifyListener(new ModifyListener() {

				@Override
				public void modifyText(ModifyEvent e) {

					checkAndModify(field, modifier, checker, defaultColor, warning);

					ObjectHandler handler = (ObjectHandler) item.getData(ObjectHandler.HANDLER);
					if (handler != null) {
						handler.updateTextRecursively(item);
						handler.persist(item);
					}
					warning.pack();
					warning.requestLayout();
				}
			});
		}

		if (fieldUsed != null){
			fieldUsed.addSelectionListener(getUsedSelectionAdapter(field, fieldUsed, modifier, item));	
		}
		
		return field;
	}
	
	private static SelectionAdapter getUsedSelectionAdapter(Text field, Button fieldUsed, TlvModifier modifier, TreeItem item){
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				field.setEnabled(fieldUsed.getSelection());
				if (!fieldUsed.getSelection()) {
					modifier.remove();
				} else {
					modifier.setValue(field.getText());
				}

				ObjectHandler handler = (ObjectHandler) item.getData(ObjectHandler.HANDLER);
				if (handler != null) {
					handler.updateTextRecursively(item);
				}
			}
		};
	}

	static void checkAndModify(Text field, TlvModifier modifier, TextFieldChecker checker, Color def, Label warning){
		FieldCheckResult check = checker.check(field);
		warning.setText(check.getReason());
		switch (check.getState()){
		case OK:
			field.setBackground(def);
			modifier.setValue(field.getText());
			break;
		case ERROR:
			field.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_RED));
			break;
		case WARNING:
			field.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_YELLOW));
			modifier.setValue(field.getText());
			break;
		default:
			break;
		}
	}
}
