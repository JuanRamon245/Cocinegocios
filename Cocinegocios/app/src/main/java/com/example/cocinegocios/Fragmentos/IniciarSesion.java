package com.example.cocinegocios.Fragmentos;

import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.cocinegocios.Clases.Usuarios;
import com.example.cocinegocios.Clases.UsuariosSQLite;
import com.example.cocinegocios.R;
import com.example.cocinegocios.SeleccionDeNegocios;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;


/**
 * Clase con funcion de fragmento para que el usuario pueda iniciar sesión en la aplicación
 * <p>
 * Esta clase o fragmento contiene la lógica necesaria para que el usuario pueda iniciar sesión en la aplicación y en el caso de que no se deslogue, podrá continuar sin tener que iniciar sesión
 * usando su cuenta.
 * <p>
 * Autor: [Juan Ramón de León Martín]
 * Fecha: [3/12/2024]
 */

public class IniciarSesion extends Fragment implements View.OnClickListener{

    private Button botonDeInicioDeSesion;
    private EditText campoGmailInicioSesion, ContraseñaInicioSesion;

    private String gmail, contraseña, primaryKey;

    SQLiteDatabase baseDatos;

    /**
     * Metodo para crear la vista del fragmento
     * <p>
     * Esta clase coge y asocia los elementos de la vista, los inicializa, y luego los asocia con el layout correspondiente. Tambien settea los botónes
     * con el setOnClickLitsener implementado de la clase. Por ultimo carga la BBDD de SQLite en caso de necesitarla para guardar los datos del usuario con el que se inicia sesión.
     *
     * @param inflater Objeto que permite crear la vista usando el Layout asociado al fragmento.
     * @param container Vista padre que contiene los elementos del fragmento
     * @param savedInstanceState Bundle que contiene la estancia del fragmento, en caso de que esté siendo recreado.
     *
     * @return view La vista ya inicializada y asociados sus elementos.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_iniciar_sesion, container, false);

        ImageView imageView = view.findViewById(R.id.imagenInicioSesion);
        imageView.setImageResource(R.drawable.iconoapp);

        campoGmailInicioSesion =  (EditText) view.findViewById(R.id.campoGmailInicioSesion);
        ContraseñaInicioSesion =  (EditText) view.findViewById(R.id.ContraseñaInicioSesion);

        botonDeInicioDeSesion = (Button) view.findViewById(R.id.botonDeInicioDeSesion);
        botonDeInicioDeSesion.setOnClickListener(this);

        UsuariosSQLite baseDatosUsuarios = new UsuariosSQLite(getContext(), "bbddUsuarios", null, 1);
        baseDatos = baseDatosUsuarios.getWritableDatabase();

        return view;
    }

    /**
     * Metodo para dar funcionalidad a los SetOnClickLitseners aasociados arriba
     * <p>
     * Esta clase coge da lógica a los botones con el SetOnClickLitseners. En este caso comprueba que el usuario introducido esté en la BBDD, comprobando si su correo existe y
     * en cuyo caso si su contraseña coincide. En el caso de ser verdadero, se inicia sesión y se guardan sus datos en SQLite para poder iniciar sesión proximamente sin tener que introductir los
     * datos. En el caso contrario, se le notificará al usuario cual ha sido su error en el intento de inicio de sesión.
     *
     *
     * @param v La vista actual de donde estamos trabajando.
     */
    @Override
    public void onClick(View v) {
        /*
         * Se da lógica al boton que sirve para iniciar sesion en la aplicacion y poder futuramente iniciar sesión sin tener que introducir ddatos, mientras unio no se deslogue.
         */
        if (v.getId() == R.id.botonDeInicioDeSesion) {
            gmail = campoGmailInicioSesion.getText().toString().trim();
            contraseña = ContraseñaInicioSesion.getText().toString();

            //Se comprueba que los campos no estén vacios
            if (!gmail.isEmpty() && !contraseña.isEmpty()) {
                //Se reemplaza los puntos por '_' porque firebase no es capaz de interpretar algunos simbolos, entonces el más comun es sustituido
                primaryKey = gmail.replace(".", "_");

                //Se busca que se exista el usuario introducido por medio de su gmail, en caso de encontrarlo, se comprueba sus contraseñas coinciden
                DatabaseReference referenciaUsuarios = FirebaseDatabase.getInstance().getReference("Usuarios");
                Query busquedaUsuarios = referenciaUsuarios.orderByChild("gmail").equalTo(primaryKey);

                busquedaUsuarios.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                                Usuarios usuario = userSnapshot.getValue(Usuarios.class);
                                if (usuario.getContraseña().equals(contraseña)) {

                                    //En caso de pasar todos los filtros se inserta el usuario en SQLite y se inicia sesión
                                    String creacionUsuario = "INSERT INTO usuario (correo, contrasena) VALUES ('"+primaryKey+"','"+contraseña+"');";
                                    baseDatos.execSQL(creacionUsuario);

                                    Intent botonParaIniciarSesion = new Intent(getActivity(), SeleccionDeNegocios.class);
                                    startActivity(botonParaIniciarSesion);
                                } else {
                                    Snackbar.make(v, "La contraseña no coincide", Snackbar.LENGTH_SHORT).show();
                                }
                            }
                        } else {
                            Snackbar.make(v, "El correo '"+gmail + "' no existe en la base de datos", Snackbar.LENGTH_SHORT).show();
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Snackbar.make(v, "Error de base de datos", Snackbar.LENGTH_SHORT).show();
                    }
                });
            }
        }
    }
}