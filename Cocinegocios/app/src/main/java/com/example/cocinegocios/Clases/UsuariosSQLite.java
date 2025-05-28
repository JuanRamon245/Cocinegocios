package com.example.cocinegocios.Clases;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

public class UsuariosSQLite  extends SQLiteOpenHelper {

    String sqlCreacion = "CREATE TABLE usuario(correo TEXT PRIMARY KEY, contrasena TEXT)";
    String sqlBorrado = "DROP TABLE IF EXISTS usuario";

    public UsuariosSQLite(@Nullable Context context, @Nullable String name, @Nullable SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }


    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(sqlCreacion);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        sqLiteDatabase.execSQL(sqlBorrado);
        sqLiteDatabase.execSQL(sqlCreacion);
    }
}