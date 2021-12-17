package com.cuenca.whatsapp_contacts;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;

/**
 * WhatsappContactsPlugin
 */
public class WhatsappContactsPlugin implements MethodCallHandler, FlutterPlugin{
    /**
     * Plugin registration.
     */
    public static void registerWith(Registrar registrar) {
        final MethodChannel channel = new MethodChannel(registrar.messenger(), "com.cuenca.plugin.whatsapp_contacts");
        channel.setMethodCallHandler(new WhatsappContactsPlugin(registrar));
    }

    WhatsappContactsPlugin(Registrar registrar) {
        this.registrar = registrar;
    }

    private final Registrar registrar;
    private MethodChannel channel;
    private FlutterPluginBinding binding;

    @Override
    public void onMethodCall(MethodCall call, Result result) {
        if (call.method.equals("getWhatsappContacts")) {
            ArrayList<HashMap<String, String>> contacts = getWhatsappContacts();
            result.success(contacts);
        } else {
            result.notImplemented();
        }
    }

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding binding) {
        channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "com.cuenca.plugin.whatsapp_contacts");
        this.binding = binding;
        channel.setMethodCallHandler(this);
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        channel.setMethodCallHandler(null);
    }

    // Implementation based on https://stackoverflow.com/questions/35448250/how-to-get-whatsapp-contacts-from-android by @mansukh-ahir
    private ArrayList<HashMap<String, String>> getWhatsappContacts() {
        Context context = registrar.context();
        ContentResolver cr = context.getContentResolver();

        Cursor contactCursor = cr.query(
                ContactsContract.RawContacts.CONTENT_URI,
                new String[]{
                        ContactsContract.RawContacts._ID,
                        ContactsContract.RawContacts.CONTACT_ID,
                },
                ContactsContract.RawContacts.ACCOUNT_TYPE + "= ?",
                new String[]{"com.whatsapp"},
                null);

        ArrayList<HashMap<String, String>> contacts = new ArrayList<>();

        if (contactCursor == null || contactCursor.getCount() == 0 || !contactCursor.moveToFirst()) {
            return contacts;
        }

        do {
            String contactId = contactCursor.getString(contactCursor.getColumnIndex(ContactsContract.RawContacts.CONTACT_ID));
            String rawContactId = contactCursor.getString(contactCursor.getColumnIndex(ContactsContract.RawContacts._ID));

            if (contactId == null || rawContactId == null) {
                continue;
            }

            Cursor whatsAppContactCursor = cr.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    new String[]{
                            ContactsContract.CommonDataKinds.Phone.NUMBER,
                            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,

                    },
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ? AND "
                            + ContactsContract.CommonDataKinds.Phone.RAW_CONTACT_ID + " = ? ",
                    new String[]{
                            contactId,
                            rawContactId,
                    },
                    null);

            if (whatsAppContactCursor == null || whatsAppContactCursor.getCount() == 0 || !whatsAppContactCursor.moveToFirst()) {
                continue;
            }

            do {
                String name = whatsAppContactCursor.getString(whatsAppContactCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                String number = whatsAppContactCursor.getString(whatsAppContactCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                HashMap<String, String> contact = new HashMap<>();
                contact.put("name", name);
                contact.put("phone_number", number);
                contacts.add(contact);
            } while (whatsAppContactCursor.moveToNext());
            whatsAppContactCursor.close();
        } while (contactCursor.moveToNext());

        contactCursor.close();

        return contacts;
    }
}
