/**
 * 
 * ====================================================================================================================================================
 * 	    "MyJDownloader Client" License
 * 	    The "MyJDownloader Client" will be called [The Product] from now on.
 * ====================================================================================================================================================
 * 	    Copyright (c) 2009-2015, AppWork GmbH <e-mail@appwork.org>
 * 	    Schwabacher Straße 117
 * 	    90763 Fürth
 * 	    Germany   
 * === Preamble ===
 * 	This license establishes the terms under which the [The Product] Source Code & Binary files may be used, copied, modified, distributed, and/or redistributed.
 * 	The intent is that the AppWork GmbH is able to provide  their utilities library for free to non-commercial projects whereas commercial usage is only permitted after obtaining a commercial license.
 * 	These terms apply to all files that have the [The Product] License header (IN the file), a <filename>.license or <filename>.info (like mylib.jar.info) file that contains a reference to this license.
 * 	
 * === 3rd Party Licences ===
 * 	Some parts of the [The Product] use or reference 3rd party libraries and classes. These parts may have different licensing conditions. Please check the *.license and *.info files of included libraries
 * 	to ensure that they are compatible to your use-case. Further more, some *.java have their own license. In this case, they have their license terms in the java file header. 	
 * 	
 * === Definition: Commercial Usage ===
 * 	If anybody or any organization is generating income (directly or indirectly) by using [The Product] or if there's as much as a 
 * 	sniff of commercial interest or aspect in what you are doing, we consider this as a commercial usage. If you are unsure whether your use-case is commercial or not, consider it as commercial.
 * === Dual Licensing ===
 * === Commercial Usage ===
 * 	If you want to use [The Product] in a commercial way (see definition above), you have to obtain a paid license from AppWork GmbH.
 * 	Contact AppWork for further details: e-mail@appwork.org
 * === Non-Commercial Usage ===
 * 	If there is no commercial usage (see definition above), you may use [The Product] under the terms of the 
 * 	"GNU Affero General Public License" (http://www.gnu.org/licenses/agpl-3.0.en.html).
 * 	
 * 	If the AGPL does not fit your needs, please contact us. We'll find a solution.
 * ====================================================================================================================================================
 * ==================================================================================================================================================== */
package org.appwork.swing.trayicon;

import java.awt.AWTException;
import java.awt.Image;
import java.awt.Point;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.appwork.utils.ImageProvider.ImageProvider;
import org.appwork.utils.event.BasicEvent;
import org.appwork.utils.event.BasicEventSender;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.EDTHelper;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.windowmanager.WindowManager;
import org.appwork.utils.swing.windowmanager.WindowManager.FrameState;

/**
 * @author thomas
 */
public class AWTrayIcon implements MouseListener, TrayMouseListener {

    public static final int                    EVENT_TOGGLE_FRAME_VISIBILITY = 0;

    public static final int                    EVENT_SHOW_POPUP              = 1;

    public static final int                    EVENT_HIDE_POPUP              = 2;

    private final JFrame                       frame;

    private ExtTrayIcon                        trayIcon;

    private TrayIconPopup                      trayIconPopup;

    private final int                          visibleToggleClickCount       = 2;

    private final BasicEventSender<AWTrayIcon> eventSender;

    public AWTrayIcon(final JFrame frame) throws AWTException {
        this(frame, frame.getIconImages() == null || frame.getIconImages().size() == 0 ? ImageProvider.createIcon(frame.getTitle() != null && frame.getTitle().length() > 0 ? frame.getTitle().charAt(0) + "" : "T", 32, 32) : frame.getIconImages().get(0));

    }

    public AWTrayIcon(final JFrame frame, final Image icon) throws AWTException {
        this(frame, icon, frame.getTitle());

    }

    /**
     * @param frame2
     * @param icon
     * @param title
     * @throws AWTException
     */
    public AWTrayIcon(final JFrame frame, final Image icon, final String title) throws AWTException {
        this.frame = frame;
        eventSender = new BasicEventSender<AWTrayIcon>();
        final SystemTray systemTray = SystemTray.getSystemTray();
        /*
         * trayicon message must be set, else windows cannot handle icon right
         * (eg autohide feature)
         */
        trayIcon = new ExtTrayIcon(icon, title);

        trayIcon.addMouseListener(this);
        trayIcon.addTrayMouseListener(this);

        systemTray.add(trayIcon);
    }

    public TrayIconPopup createPopup() {
        return null;
    }

    public void displayToolTip() {
        System.out.println("Tooltip");
        // trayIcon.getEstimatedTopLeft();
    }

    public void dispose() {

        new EDTRunner() {

            @Override
            protected void runInEDT() {
                try {
                    if (trayIcon != null) {

                        SystemTray.getSystemTray().remove(trayIcon);
                        trayIcon.removeMouseListener(AWTrayIcon.this);
                        trayIcon.removeTrayMouseListener(AWTrayIcon.this);
                        trayIcon = null;
                        AWTrayIcon.this.hideToolTip();
                        if (trayIconPopup != null) {
                            trayIconPopup.dispose();

                            trayIconPopup = null;
                            eventSender.fireEvent(new BasicEvent<AWTrayIcon>(AWTrayIcon.this, AWTrayIcon.EVENT_HIDE_POPUP, AWTrayIcon.this, null));
                        }
                    }
                } catch (final Exception e) {
                }
            }

        };

    }

    /**
     * @return the eventSender
     */
    public BasicEventSender<AWTrayIcon> getEventSender() {
        return eventSender;
    }

    public JFrame getFrame() {
        return frame;
    }

    private void hideToolTip() {
    }

    public boolean isFrameVisible() {
        return frame != null && frame.isVisible();
    }

    public void mouseClicked(final MouseEvent e) {
    }

    public void mouseEntered(final MouseEvent e) {
    }

    public void mouseExited(final MouseEvent e) {
        hideToolTip();
    }

    public void mouseMoveOverTray(final MouseEvent me) {
        if (trayIconPopup != null && trayIconPopup.isVisible()) { return; }
        displayToolTip();
    }

    public void mousePressed(final MouseEvent e) {
        hideToolTip();

        if (e.getSource() instanceof TrayIcon) {
            if (!CrossSystem.isMac()) {
                if (e.getClickCount() == visibleToggleClickCount && !SwingUtilities.isRightMouseButton(e)) {
                    eventSender.fireEvent(new BasicEvent<AWTrayIcon>(this, AWTrayIcon.EVENT_TOGGLE_FRAME_VISIBILITY, this, null));

                    onToggleVisibility();

                } else {
                    if (trayIconPopup != null && trayIconPopup.isShowing()) {
                        trayIconPopup.dispose();

                        trayIconPopup = null;
                        eventSender.fireEvent(new BasicEvent<AWTrayIcon>(this, AWTrayIcon.EVENT_HIDE_POPUP, this, null));

                    } else if (SwingUtilities.isRightMouseButton(e)) {

                        trayIconPopup = createPopup();
                        if (trayIconPopup == null) { return; }

                        trayIconPopup.setPosition(e.getPoint());
                        WindowManager.getInstance().setVisible(trayIconPopup, true,FrameState.OS_DEFAULT);
                        trayIconPopup.startAutoHide();
                        eventSender.fireEvent(new BasicEvent<AWTrayIcon>(this, AWTrayIcon.EVENT_SHOW_POPUP, this, null));

                    }
                }
            } else {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    if (e.getClickCount() == visibleToggleClickCount && !SwingUtilities.isLeftMouseButton(e)) {
                        eventSender.fireEvent(new BasicEvent<AWTrayIcon>(this, AWTrayIcon.EVENT_TOGGLE_FRAME_VISIBILITY, this, null));

                        onToggleVisibility();
                    } else {
                        if (trayIconPopup != null && trayIconPopup.isShowing()) {
                            trayIconPopup.dispose();
                            trayIconPopup = null;
                            eventSender.fireEvent(new BasicEvent<AWTrayIcon>(this, AWTrayIcon.EVENT_HIDE_POPUP, this, null));

                        } else if (SwingUtilities.isLeftMouseButton(e)) {

                            trayIconPopup = createPopup();
                            if (trayIconPopup == null) { return; }
                            final Point pointOnScreen = e.getLocationOnScreen();
                            if (e.getX() > 0) {
                                pointOnScreen.x -= e.getPoint().x;
                            }

                            trayIconPopup.setPosition(pointOnScreen);
                            WindowManager.getInstance().setVisible(trayIconPopup, true,FrameState.OS_DEFAULT);
                            trayIconPopup.startAutoHide();
                            eventSender.fireEvent(new BasicEvent<AWTrayIcon>(this, AWTrayIcon.EVENT_SHOW_POPUP, this, null));

                        }
                    }
                }
            }
        }
    }

    public void mouseReleased(final MouseEvent e) {
    }

    /**
     * 
     */
    public void onToggleVisibility() {
        setFrameVisible(!isFrameVisible());
    }

    public void setFrameVisible(final boolean visible) {
        if (frame == null) { return; }
        new EDTHelper<Object>() {

            @Override
            public Object edtRun() {

                WindowManager.getInstance().setVisible(frame, visible,FrameState.OS_DEFAULT);

                return null;
            }

        }.start();
    }

    public void setImage(final Image icon) {
        trayIcon.setImage(icon);
    }

    public void setToolTip(final String tooltip) {
        trayIcon.setToolTip(tooltip);
    }

}
