package org.percepta.mgrankvi.client.map;

import com.google.gwt.animation.client.Animation;
import com.google.gwt.canvas.client.Canvas;
import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.dom.client.MouseEvent;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseMoveHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseUpEvent;
import com.google.gwt.event.dom.client.MouseUpHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.Widget;
import org.percepta.mgrankvi.client.floor.RoomContainer;
import org.percepta.mgrankvi.client.geometry.Point;
import org.percepta.mgrankvi.client.helpers.Clicked;
import org.percepta.mgrankvi.client.helpers.Extents;
import org.percepta.mgrankvi.client.room.RoomWidget;
import org.percepta.mgrankvi.client.table.TableWidget;
import org.percepta.mgrankvi.client.utils.GridUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Mikael Grankvist - Vaadin Ltd
 */
public class SeatingMapWidget extends Composite implements ClickHandler, MouseDownHandler, MouseUpHandler, MouseMoveHandler, MouseOutHandler,
        ChangeHandler, KeyUpHandler {

    private static final String CLASSNAME = "c-seating-map";
    private static final int GRID_SIZE = 50;

    protected final Canvas canvas;

//  private final TextBox typeAndEdit = new TextBox();

    protected PopupPanel contextMenu;

    private int gridSize = 50;
    private int offsetX = 0;
    private int offsetY = 0;
    private final Point origo = new Point(0, 0);

    private int orgX, orgY;
    private Point orgOrigo = null;

    final int BUTTON_SIZE = 25;
    final String DEFAULT_COLOR = "LAVENDER";
    final String DEFAULT_SELECTED_COLOR = "SILVER";

    final GridButton up, down, plus, minus;

    private boolean internalSearchBarEnabled = true;
    SearchBar searchBar = new SearchBar(this);
    ButtonBar buttonBar = new ButtonBar(this);

    private final List<GridButton> buttons;

    protected boolean animating = false;
    private boolean mouseDown = false;
    private boolean mouseMoved = true;
    private boolean hasFloorAbove = false;
    private boolean hasFloorBelow = false;

    private int downX = 0;
    private int downY = 0;
    int zoom = 0;

    //  boolean isEditable = false;
    boolean pathing = false;
    private final FlowPanel content;

    RoomContainer selectedFloor;
    LinkedList<RoomContainer> floors = new LinkedList<RoomContainer>();
    // TOOD: This should be ficed to items
    List<Object> items = new LinkedList<Object>();
//  CItem pathFromTo;
//  GeneratedCodePopup codePopup;
//  PathGridItem pathGrid;

    private final ActionListener actionListener;

    public SeatingMapWidget(ActionListener actionListener) {
        this.actionListener = actionListener;
        content = new FlowPanel();
        content.setSize("100%", "100%");

        initWidget(content);

        setStyleName(CLASSNAME);

        addDomHandler(this, MouseDownEvent.getType());
        addDomHandler(this, MouseMoveEvent.getType());
        addDomHandler(this, MouseUpEvent.getType());
        addDomHandler(this, ClickEvent.getType());
        addDomHandler(this, ChangeEvent.getType());
        addDomHandler(this, MouseOutEvent.getType());
        addDomHandler(this, KeyUpEvent.getType());

        canvas = Canvas.createIfSupported();
        if (canvas != null) {
            content.add(canvas);
            clearCanvas();
        } else {
            getElement().setInnerHTML("Canvas not supported");
        }

//    content.add(typeAndEdit);
//
//    final Style editStyle = typeAndEdit.getElement().getStyle();
//    typeAndEdit.addChangeHandler(this);
//    // typeAndEdit.addKeyUpHandler(this);
//    editStyle.setPosition(Style.Position.RELATIVE);
//    editStyle.setLeft(0.0, Style.Unit.PX);
//    editStyle.setProperty("margin", "0");
//    typeAndEdit.setWidth(Window.getClientWidth() + "px");

        plus = new GridButton(new Point(Window.getClientWidth() - 50, 25), BUTTON_SIZE, DEFAULT_COLOR);
        minus = new GridButton(new Point(Window.getClientWidth() - 50, 51), BUTTON_SIZE, DEFAULT_COLOR);
        up = new GridButton(new Point(Window.getClientWidth() - 50, 78), BUTTON_SIZE, DEFAULT_COLOR);
        down = new GridButton(new Point(Window.getClientWidth() - 50, 105), BUTTON_SIZE, DEFAULT_COLOR);
        buttons = Arrays.asList(up, down, plus, minus);
    }


    private void clearCanvas() {
        canvas.setCoordinateSpaceWidth(Window.getClientWidth());
        canvas.setCoordinateSpaceHeight(Window.getClientHeight() - 4);// - typeAndEdit.getOffsetHeight());
    }

    public void setFloor(final RoomContainer floor) {
        selectedFloor = floor;
        floor.setGrid(this);
        hasFloorAbove = hasFloorAbove();
        hasFloorBelow = hasFloorBelow();

        actionListener.setSelectedFloor(floor.getLevel());
    }

    private void paint() {
        final Context2d context = canvas.getContext2d();

        GridUtils.paintZoomInButton(context, plus.getPosition(), plus.getSize(), plus.getColor());
        GridUtils.paintZoomOutButton(context, minus.getPosition(), minus.getSize(), minus.getColor());

        if (floors.size() > 1) {
            if (hasFloorAbove) {
                GridUtils.paintFloorUpButton(context, up.getPosition(), up.getSize(), up.getColor());
            }
            if (hasFloorBelow) {
                GridUtils.paintFloorDownButton(context, down.getPosition(), down.getSize(), down.getColor());
            }
        }

        if (selectedFloor != null) {
            selectedFloor.paint();
        }

//    for (final VisualItem item : items) {
//      item.paint(context);
//    }
        if (internalSearchBarEnabled)
            searchBar.paint(context);
        buttonBar.paint(context);

        GridUtils.paintGrid(context, new Point(offsetX, offsetY), gridSize, origo);
    }

    public Canvas getCanvas() {
        return canvas;
    }

    private boolean hasFloorAbove() {
        final int selectedFloorIndex = floors.indexOf(selectedFloor);
        return selectedFloorIndex < floors.size() - 1;
    }

    private boolean hasFloorBelow() {
        return !selectedFloor.equals(floors.getFirst());
    }

    public void repaint() {
        clearCanvas();
        paint();
    }

    @Override
    public void onClick(final ClickEvent event) {
        if (animating) {
            return;
        }
//    if (typeAndEdit.getElement().equals(event.getNativeEvent().getEventTarget())) {
//      return;
//    }
        if (event.getNativeButton() == NativeEvent.BUTTON_RIGHT) {
        } else {
            if (mouseMoved) {
                mouseMoved = false;
                return;
            }
            final int clientX = getEventX(event);
            final int clientY = getEventY(event);
            if (clientX > Window.getClientWidth() - 50 && clientX < Window.getClientWidth() - 25) {
                if (clientY > plus.getPosition().getY() && clientY < plus.getPosition().getY() + BUTTON_SIZE) {
                    // sx = sy = 2
                    // x2 = sx*x1
                    // y2 = sy*y1
                    zoom++;
                    if (zoom == 0) {
                        reset();
                    } else {
                        scale(2);
                    }
                    repaint();
                    return;
                } else if (clientY > minus.getPosition().getY() && clientY < minus.getPosition().getY() + BUTTON_SIZE) {
                    // sx = sy = 0.5
                    // x2 = sx*x1
                    // y2 = sy*y1
                    zoom--;
                    if (zoom == 0) {
                        reset();
                    } else {
                        scale(0.5);
                    }
                    repaint();
                    return;
                } else if (hasFloorAbove && clientY > up.getPosition().getY() && clientY < up.getPosition().getY() + BUTTON_SIZE) {
                    final int selectedFloorIndex = floors.indexOf(selectedFloor);
                    setFloor(floors.get(selectedFloorIndex + 1));
//          if (pathFromTo != null) {
//            items.remove(pathFromTo);
//          }
                    repaint();
                    return;
                } else if (hasFloorBelow && clientY > down.getPosition().getY() && clientY < down.getPosition().getY() + BUTTON_SIZE) {
                    final int selectedFloorIndex = floors.indexOf(selectedFloor);
                    setFloor(floors.get(selectedFloorIndex - 1));
//          if (pathFromTo != null) {
//            items.remove(pathFromTo);
//          }
                    repaint();
                    return;
                }
            }

            if (selectedFloor != null) {
//                selectedFloor.click(clientX, clientY);
                actionListener.clicked(selectedFloor.clickAction(downX, downY));
            }
//      if (pathing) {
//        final int x = (int) (clientX - origo.getX());
//        final int y = (int) (clientY - origo.getY());
//        if (!pathGrid.hasSelected() && pathGrid.pointInObject(x, y)) {
//          pathGrid.selectNode(x, y);
//        } else if (pathGrid.hasSelected()) {
//          pathGrid.link(x, y);
//        } else {
//          // path.addPoint(x, y);
//          Logger.getLogger("path").log(Level.ALL, "Path point position: " + x + "," + y);
//          pathGrid.addNode(new Node(x + y, new Point(x, y)));
//        }
//      }
            buttonBar.click(clientX, clientY);
            repaint();
        }
    }

    private int getEventY(MouseEvent event) {

        return event.getClientY() - this.getAbsoluteTop();
    }

    private int getEventX(MouseEvent event) {
        return event.getClientX() - this.getAbsoluteLeft();
    }

    private void scale(final double scale) {
        if (orgOrigo == null) {
            orgOrigo = new Point(origo.getX(), origo.getY());
            orgX = offsetX;
            orgY = offsetY;
        }
        // if (selectedFloor != null) {
        // selectedFloor.scale(scale);
        // }
        for (final RoomContainer floor : floors) {
            floor.scale(scale);
        }
        offsetX = (int) Math.ceil(offsetX * scale);
        offsetY = (int) Math.ceil(offsetY * scale);
        gridSize = (int) Math.ceil(gridSize * scale);
        origo.setX((int) Math.ceil(origo.getX() * scale));
        origo.setY((int) Math.ceil(origo.getY() * scale));
    }

    private void reset() {
        origo.setX(orgOrigo.getX());
        origo.setY(orgOrigo.getY());
        gridSize = GRID_SIZE;
        offsetX = orgX;
        offsetY = orgY;
        orgOrigo = null;

        // if (selectedFloor != null) {
        // selectedFloor.reset();
        // }
        for (final RoomContainer floor : floors) {
            floor.reset();
        }
    }

    @Override
    public void onMouseDown(final MouseDownEvent event) {
        if (animating) {
            return;
        }
        downX = getEventX(event);
        downY = getEventY(event);
        mouseDown = true;
        if (contextMenu != null) {
            contextMenu.hide();
            contextMenu = null;
        }
    }

    @Override
    public void onMouseUp(final MouseUpEvent event) {
        mouseDown = false;
        if (selectedFloor != null) {
            selectedFloor.mouseUp();
        }
    }

    @Override
    public void onMouseMove(final MouseMoveEvent event) {
        if (animating) {
            return;
        }
        final int clientX = getEventX(event);
        final int clientY = getEventY(event);
        if (mouseDown) {
            mouseMoved = true;

            pan(event);

            downX = clientX;
            downY = clientY;

            repaint();
        } else if (selectedFloor != null) {
            selectedFloor.checkHover(clientX, clientY);
        }

        // Change coloring of + and - buttons if hovered over.
        if (clientX > Window.getClientWidth() - 50 && clientX < Window.getClientWidth() - 25) {
            if (clientY > plus.getPosition().getY() && clientY < plus.getPosition().getY() + BUTTON_SIZE) {
                plus.setColor(DEFAULT_SELECTED_COLOR);
                clearColors(plus);
            } else if (clientY > minus.getPosition().getY() && clientY < minus.getPosition().getY() + BUTTON_SIZE) {
                minus.setColor(DEFAULT_SELECTED_COLOR);
                clearColors(minus);
            } else if (hasFloorAbove && clientY > up.getPosition().getY() && clientY < up.getPosition().getY() + BUTTON_SIZE) {
                up.setColor(DEFAULT_SELECTED_COLOR);
                clearColors(up);
            } else if (hasFloorBelow && clientY > down.getPosition().getY() && clientY < down.getPosition().getY() + BUTTON_SIZE) {
                down.setColor(DEFAULT_SELECTED_COLOR);
                clearColors(down);
            } else {
                clearColors();
            }
        } else {
            clearColors();
        }
        if (internalSearchBarEnabled) {
            if (searchBar.mouseOver(clientX, clientY)) {
                if (!searchBar.isVisible()) {
                    searchBar.setAnimate(true);
                    searchBar.setVisible(true);
                }
            } else if (searchBar.isVisible()) {
                searchBar.setAnimate(true);
                searchBar.setVisible(false);
            }
        }
        repaint();
    }

    protected void clearColors(final GridButton... not) {
        final List<GridButton> notList = Arrays.asList(not);
        for (final GridButton button : buttons) {
            if (!notList.contains(button)) {
                button.setColor(DEFAULT_COLOR);
            }
        }
    }

    public void pan(final MouseMoveEvent event) {
        pan(getEventX(event) - downX, getEventY(event) - downY);
    }

    public void pan(final int amountx, final int amounty) {
        offsetX += amountx;
        offsetY += amounty;

        offsetX = offsetX % 50;
        offsetY = offsetY % 50;

        origo.move(amountx, amounty);
        if (orgOrigo != null) {
            orgOrigo.move(amountx, amounty);
        }
        for (final RoomContainer floor : floors) {
            floor.pan(amountx, amounty);
        }

//    for (final VisualItem item : items) {
//      item.movePosition(amountx, amounty);
//    }
    }

    boolean noChangeEvent = false;

    @Override
    public void onKeyUp(final KeyUpEvent event) {
        if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
            noChangeEvent = true;
//      handleTextFieldValue(null);
        }
    }

    @Override
    public void onChange(final ChangeEvent event) {
        if (noChangeEvent) {
            noChangeEvent = false;
        } else {
//      handleTextFieldValue(null);
        }
    }

    public void setAnimating(final boolean animating) {
        this.animating = animating;
    }

    protected void handleTextFieldValue(String value) {
//    if (value == null) {
//      value = typeAndEdit.getValue();
//      noChangeEvent = true;
//      typeAndEdit.setValue("");
//    }

        if (value != null && !value.isEmpty()) {
            final CommandObject cmd = new CommandObject(value);
            if (cmd.getCommand().equals(CommandObject.Command.INVALID_STRING)) {
                return;
            }
//      if (selectedFloor != null) {
//        for (final CRoom room : selectedFloor.getRooms()) {
//          if (room.isSelected()) {
//            switch (cmd.getCommand()) {
//              case MOVE_TO:
//                room.setPosition(GeometryUtil.combine(origo, cmd.getPosition()));
//                break;
//              case MOVE_BY:
//                room.movePosition(cmd.getX(), cmd.getY());
//                break;
//              case SAVE:
//                fireEvent(new MenuEvent(MenuEvent.MenuEventType.UPDATE_ROOMS));
//                break;
//              case INVALID_STRING:
//                Window.alert("Command String was invalid");
//                break;
//              case PARSE_FAILED:
//                Window.alert("Parsing coordinates failed");
//                break;
//            }
//            repaint();
//            break;
//          }
//        }
//      }
            switch (cmd.getCommand()) {
                case PAN:
                    pan(cmd.getX(), cmd.getY());
                    repaint();
                    break;
//        case EDIT:
//          setEditable(!isEditable);
//          break;
//        case PATHING:
//          setPathing(!pathing);
//          if (pathing && codePopup == null) {
//            codePopup = new GeneratedCodePopup();
//            codePopup.show();
//            if (selectedFloor != null && selectedFloor.waypoints != null) {
//              // items.add(selectedFloor.waypoints);
//              pathGrid = selectedFloor.waypoints;
//              selectedFloor.updateWaypoints(false);
//              selectedFloor.waypoints.setPathPopup(codePopup);
//            } else {
//              pathGrid = new PathGridItem(codePopup);
//              pathGrid.setPosition(new Point(origo.getX(), origo.getY()));
//            }
//            items.add(pathGrid);
//            repaint();
//          } else {
//            codePopup.hide();
//            codePopup = null;
//            items.remove(pathGrid);
//            pathGrid = null;
//            if (selectedFloor != null && selectedFloor.waypoints != null) {
//              items.remove(selectedFloor.waypoints);
//              selectedFloor.updateWaypoints(true);
//            }
//            repaint();
//          }
//          break;
                case FIND:
                    actionListener.find(cmd.getValue());

//          // TODO: Handle all floors.
//          Logger.getLogger("TextHandle").log(Level.FINE, "handling search: " + cmd.getValue());
//          if (selectedFloor != null) {
//            if (selectedFloor.getNames().contains(cmd.getValue())) {
//              selectedFloor.markTableOfSelectedPerson(cmd.getValue());
//            } else if (selectedFloor.namesContain(cmd)) {
//              final LinkedList<String> possible = selectedFloor.possibilities(cmd);
//              if (possible.size() == 1) {
//                selectedFloor.markTableOfSelectedPerson(possible.getFirst());
//              } else if (possible.size() > 1) {
////                new NameSelectPopup(possible, selectedFloor);
//              }
//            } else {
//              for (final RoomContainer floor : floors) {
//                if (floor.namesContain(cmd)) {
//                  final LinkedList<String> possible = floor.possibilities(cmd);
//                  if (possible.size() == 1) {
//                    final int selectedFloorIndex = floors.indexOf(floor);
//                    setFloor(floors.get(selectedFloorIndex));
//                    floor.markTableOfSelectedPerson(possible.getFirst());
//                    return;
//                  } else if (possible.size() > 1) {
////                    new NameSelectPopup(possible, floor);
//                    return;
//                  }
//                }
//              }
//              final VNotification notification = new VNotification();
//              final Style style = notification.getElement().getStyle();
//              style.setBackgroundColor("#c8ccd0");
//              style.setPadding(15.0, Style.Unit.PX);
//              style.setProperty("border-radius", "4px");
//              style.setProperty("-moz-border-radius", "4px");
//              style.setProperty("-webkit-border-radius", "4px");
//
//              notification.show("No user found for [" + cmd.getValue() + "]", VNotification.CENTERED, null);
//            }
//          }
                    break;
            }
        }
    }


    @Override
    public void onMouseOut(final MouseOutEvent event) {
        mouseDown = false;
        if (selectedFloor != null) {
//      selectedFloor.setSelected(null);
        }
    }

    public void setPathing(final boolean pathing) {
        this.pathing = pathing;
    }

    public void clear() {
        floors.clear();
        items.clear();
    }

    public void add(final Widget widget) {
        if (widget instanceof RoomContainer) {
            floors.add((RoomContainer) widget);
            Collections.sort(floors);
//    } else if (widget instanceof VisualItem) {
//      items.add((VisualItem) widget);
        }
    }

    public void showNames() {
        if (selectedFloor != null) {
            selectedFloor.showNames();
        }
    }

//  public void getPath() {
//    new PathSearchPopup(floors, this);
//  }

//  public void getPath(final int fromId, final int toId) {
//    if (pathFromTo != null) {
//      items.remove(pathFromTo);
//    }
//    pathFromTo = selectedFloor.waypoints.getPath(fromId, toId);
//    if (pathFromTo != null) {
//      items.add(pathFromTo);
//    }
//    repaint();
//  }


    public boolean isAnimating() {
        return animating;
    }

    protected interface ActionListener {
        void find(String searchString);

        void setSelectedFloor(int floor);

        void clicked(Clicked clicked);
    }

    protected void moveTableToView(String tableId) {
        for (RoomContainer floor : floors) {
            for (RoomWidget room : floor.getRooms()) {
                for (TableWidget table : room.getTables()) {
                    if (table.id.equals(tableId)) {
                        if(!selectedFloor.equals(floor)){
                            setFloor(floor);
                        }
                        moveTableToView(table);
                        return;
                    }
                }
            }
        }
    }

    protected void moveTableToView(final TableWidget table) {
        Extents extents = table.getExtents();
        final double xPointInCanvas = (canvas.getCoordinateSpaceWidth() / 2) - (extents.getMaxX() - extents.getMinX()) / 2;
        final double yPointInCanvas = (canvas.getCoordinateSpaceHeight() / 2) - (extents.getMaxY() - extents.getMinY()) / 2;

        final double tableCornerX = table.getXPositionOnCanvas();
        final double tableCornerY = table.getYPositionOnCanvas();

        final double panX = xPointInCanvas - tableCornerX;
        final double panY = yPointInCanvas - tableCornerY;

        final Animation animate = new Animation() {
            double movedX = 0;
            double movedY = 0;

            @Override
            protected void onUpdate(final double progress) {
                final double moveX = panX * progress - movedX;
                final double moveY = panY * progress - movedY;
                movedX += moveX;
                movedY += moveY;
                pan((int) Math.floor(moveX), (int) Math.floor(moveY));
                repaint();
            }

            @Override
            protected void onComplete() {
                super.onComplete();
                setAnimating(false);
            }
        };
        setAnimating(true);
        Logger.getLogger("CFloor").log(Level.FINER, " -- X: " + panX + " Y: " + panY);
        animate.run(Math.abs(panX) > Math.abs(panY) ? (int) (Math.abs(panX)) : (int) (Math.abs(panY)));
    }
}
