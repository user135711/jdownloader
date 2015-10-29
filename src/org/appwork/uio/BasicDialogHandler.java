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
package org.appwork.uio;

import javax.swing.Icon;

import org.appwork.exceptions.WTFException;
import org.appwork.utils.locale._AWU;
import org.appwork.utils.swing.dialog.AbstractDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;

public class BasicDialogHandler implements UserIOHandlerInterface {
    private static final Dialog D = Dialog.I();

    public boolean showConfirmDialog(final int flags, final String title, final String message, final Icon icon, final String ok, final String cancel) {
        try {
            D.showConfirmDialog(flags, title, message, icon, ok, cancel);
            return true;
        } catch (final DialogClosedException e) {
            // // TODO Auto-generated catch block
            // e.printStackTrace();
        } catch (final DialogCanceledException e) {
            // // TODO Auto-generated catch block
            // e.printStackTrace();
        }
        return false;
    }

    public boolean showConfirmDialog(final int flags, final String title, final String message) {
        return showConfirmDialog(flags, title, message, null, null, null);
    }

    public void showMessageDialog(final String message) {
        D.showMessageDialog(message);
    }

    // @SuppressWarnings("unchecked")
    public <T extends UserIODefinition> T show(final Class<T> class1, final T impl) {
        try {
            if (impl instanceof AbstractDialog) {
                D.showDialog((AbstractDialog<?>) impl);
            } else {
                throw new WTFException("Not Supported Dialog Type!: " + impl);
            }
        } catch (final DialogClosedException e) {

            // no Reason to log here
        } catch (final DialogCanceledException e) {
            // no Reason to log here
        }
        return impl;
    }

    public void showErrorMessage(final String message) {
        D.showErrorDialog(message);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.appwork.uio.UserIOHandlerInterface#showException(java.lang.String, java.lang.Throwable)
     */
    @Override
    public void showException(String message, Throwable e) {

        D.showExceptionDialog(_AWU.T.DIALOG_ERROR_TITLE(), message, e);
    }

}
