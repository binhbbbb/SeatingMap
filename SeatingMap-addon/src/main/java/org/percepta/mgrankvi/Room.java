package org.percepta.mgrankvi;

import com.vaadin.ui.Component;
import org.percepta.mgrankvi.client.geometry.Line;
import org.percepta.mgrankvi.client.room.RoomState;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

/**
 * Created by Mikael on 18/12/16.
 */
public class Room extends AbstractComoponents {

    private List<Table> tables = new LinkedList<>();

    public Room() {
        setId(UUID.randomUUID().toString());
    }

    public Room(List<Table> tables) {
        this();
        this.tables = tables;
    }

    public void addLines(List<Line> lines) {
        getState().lines = lines;
    }

    public void setId(String id) {
        getState().id = id;
    }


    public Table getTableById(String id) {
        if (id != null) {
            for (Table table : tables) {
                if (table.getId().equals(id)) {
                    return table;
                }
            }
        }
        return null;
    }
    @Override
    protected RoomState getState() {
        return (RoomState) super.getState();
    }

    @Override
    public void addComponent(Component c) {
        super.addComponent(c);
        if(c instanceof Table && !tables.contains((Table)c)) {
            tables.add((Table)c);
        }
    }

    @Override
    public void removeComponent(Component c) {
        super.removeComponent(c);
        tables.remove(c);
    }

    @Override
    public void removeAllComponents() {
        super.removeAllComponents();
        tables.clear();
    }

    public List<Table> getTables() {
        return tables;
    }
}
