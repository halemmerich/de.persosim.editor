package de.persosim.editor.ui.editor.handlers;

import java.util.Arrays;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

import de.persosim.editor.ui.editor.checker.HexChecker;
import de.persosim.simulator.tlv.ConstructedTlvDataObject;
import de.persosim.simulator.tlv.PrimitiveTlvDataObject;
import de.persosim.simulator.tlv.TlvConstants;
import de.persosim.simulator.tlv.TlvDataObjectContainer;
import de.persosim.simulator.utils.HexString;

public class OptionalDataHandler extends AbstractObjectHandler {

	@Override
	public boolean canHandle(Object object) {
		if (object instanceof ConstructedTlvDataObject) {
			ConstructedTlvDataObject sequence = (ConstructedTlvDataObject) object;
			return TlvConstants.TAG_SEQUENCE.equals(sequence.getTlvTag());
		}
		return false;
	}

	@Override
	public void createItem(TreeItem parentItem, Object object, HandlerProvider provider) {
		if (object instanceof ConstructedTlvDataObject) {
			TreeItem item = getItem(parentItem);
			handleItem((ConstructedTlvDataObject) object, provider, item);
		}
	}

	private TreeItem getItem(TreeItem parentItem) {
		return new TreeItem(parentItem, SWT.NONE);
	}

	protected void handleItem(ConstructedTlvDataObject tlv, HandlerProvider provider, TreeItem item) {
		item.setData(tlv);
		setText(item);
		item.setData(HANDLER, this);
	}

	@Override
	public void createItem(Tree parentTree, Object object, HandlerProvider provider) {
		if (object instanceof ConstructedTlvDataObject) {
			TreeItem item = new TreeItem(parentTree, SWT.NONE);
			handleItem((ConstructedTlvDataObject) object, provider, item);
		}
	}

	@Override
	public void setText(TreeItem item) {
		item.setText("Optional Data");
	}

	@Override
	protected String getType() {
		return "Optional data";
	}

	@Override
	protected void createEditingComposite(Composite composite, TreeItem item) {
		composite.setLayout(new GridLayout(2, false));

		TlvModifier modifier = new HexStringTlvModifier(
				(PrimitiveTlvDataObject) ((ConstructedTlvDataObject) item.getData())
						.getTlvDataObject(TlvConstants.TAG_OID));

		EditorFieldHelper.createField(item, true, composite, modifier, new HexChecker(), "OID");

		ConstructedTlvDataObject ctlv = (ConstructedTlvDataObject) item.getData();

		EditorFieldHelper.createBinaryField(item, false, composite, new TlvModifier() {

			@Override
			public void setValue(String value) {
				remove();
				try {
					ctlv.addAll(new TlvDataObjectContainer(HexString.toByteArray(value)).getTlvObjects());
					((ObjectHandler) item.getData(HANDLER)).persist(item);
				} catch (Exception e) {
					// ignore wrong user input, UI will show hints for correction
				}
			}

			@Override
			public void remove() {
				if (ctlv.getNoOfElements() > 1) {
					PrimitiveTlvDataObject toSave = (PrimitiveTlvDataObject) ctlv.getTlvDataObject(TlvConstants.TAG_06);
					ctlv.removeAllTlvDataObjects();
					ctlv.addTlvDataObject(toSave);
				}
			}

			@Override
			public String getValue() {
				byte[] valueField = ctlv.getValueField();
				return HexString.encode(Arrays.copyOfRange(valueField, ctlv.getTlvDataObject(TlvConstants.TAG_06).getLength(), valueField.length));
			}
		}, new HexChecker(), "Content as defined by above OID");
	}

	@Override
	public void createMenu(Menu menu, TreeItem item) {
		MenuItem mitem = new MenuItem(menu, SWT.NONE);
		mitem.setText("Remove optional data");
		mitem.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				((ObjectHandler)item.getParentItem().getData(HANDLER)).removeItem(item);
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// TODO Auto-generated method stub
				
			}
		});
	}
}
