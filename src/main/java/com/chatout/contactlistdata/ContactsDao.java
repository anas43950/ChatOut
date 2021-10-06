package com.chatout.contactlistdata;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.chatout.models.Contact;

import java.util.List;

@Dao
public interface ContactsDao {
    @Query("SELECT * FROM contacts")
    LiveData<List<Contact>> loadAllContacts();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertContact(Contact contact);

    @Delete
    void deleteContact(Contact contact);

    @Query("SELECT * FROM contacts WHERE id = :uid ")
    Contact loadContactById(String uid);

    @Query("DELETE FROM contacts WHERE id = :uid")
    void deleteContactById(String uid);
}
