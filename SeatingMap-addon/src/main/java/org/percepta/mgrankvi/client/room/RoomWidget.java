package org.percepta.mgrankvi.client.room;

import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.dom.client.Document;
import com.google.gwt.user.client.ui.Widget;
import org.percepta.mgrankvi.client.abstracts.Targetable;
import org.percepta.mgrankvi.client.table.TableWidget;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by Mikael on 18/12/16.
 */
public class RoomWidget extends Targetable {

    protected List<TableWidget> tables = new LinkedList<>();

    public RoomWidget() {  // Dummy
        setElement(Document.get().createDivElement());
    }


    public void clear() {
        tables.clear();
    }

    public void add(final Widget widget) {
        if (widget instanceof TableWidget) {
            tables.add((TableWidget) widget);
        }
    }

    @Override
    public void movePosition(final int amountx, final int amounty) {
        super.movePosition(amountx, amounty);
        for (final TableWidget table : tables) {
            table.movePosition(amountx, amounty);
        }
    }
    public void paint(Context2d context) {
        super.paint(context);

        for (final TableWidget table : tables) {
            table.paint(context);
        }
    }

    public void checkHover(final double clientX, final double clientY) {
        for (final TableWidget table : tables) {
            table.setPaintName(table.pointInObject(clientX, clientY));
        }
    }
}
