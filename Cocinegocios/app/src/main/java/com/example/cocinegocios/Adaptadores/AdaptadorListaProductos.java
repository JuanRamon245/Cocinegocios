package com.example.cocinegocios.Adaptadores;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.example.cocinegocios.Clases.Producto;
import com.example.cocinegocios.Clases.UsuariosSQLite;
import com.example.cocinegocios.EditarProductos;
import com.example.cocinegocios.R;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.DecimalFormat;
import java.util.ArrayList;

/**
 * Clase con funcion de adaptador para las listas que contengan productos del negocio.
 * <p>
 * Esta clase o adaptador contiene la lógica necesaria para mostrar los productos que haya en el negocio donde él usuario esté actualmente.
 * <p>
 * Autor: [Juan Ramón de León Martín]
 * Fecha: [3/12/2024]
 */

public class AdaptadorListaProductos extends ArrayAdapter<Producto> {

    private ArrayList<Producto> lista;
    private Context mContext;

    private String correoUsuario, correoNegocio, rolUsuario;

    private StorageReference mStorage;

    SQLiteDatabase baseDatos;

    private Bundle negocioSeleccionado;

    /**
     * Constructor del adaptador de la lista de productos.
     * <p>
     * Este constructor inicializa el adaptador con el contexto, la lista de productos, y el nombre del negocio en el que estamos actualmente.
     *
     * @param contexto El contexto de la actividad o fragmento donde se utiliza el adaptador.
     * @param lista La lista de objetos de tipo Producto que se mostrarán en la vista.
     * @param nombreNegocio El nombre del negocio en el que estamos actualmente.
     */
    public AdaptadorListaProductos(Context contexto, ArrayList<Producto> lista, String nombreNegocio) {
        super(contexto, R.layout.elemento_producto, lista);
        this.lista = lista;
        this.mContext = contexto;
        this.mStorage = FirebaseStorage.getInstance().getReference();
        this.correoUsuario = nombreNegocio;
    }

    /**
     * Metodo para inicializar el contenido del listado de productos.
     * <p>
     * Esta clase inicializa el contenido de cada uno de los productos de la lista de productos.
     */
    public static class ViewHolder {
        TextView nombreProducto;
        TextView precioProducto;
        TextView descripcionProducto;
        TextView pasosASeguir;
        ImageView imagenProducto;
        Button botonMasInformacionProducto;
    }

    /**
     * Metodo para dar utilidad a los productos
     * <p>
     * Esta clase coge y asocia los contenidos de cada elemento producto y los asocia con los del 'elemento_producto' mediante los ViewHolder. El ViewHolder se crea si no hay uno previamente,en el
     * caso contrario se reutiliza. Luego se pone los datos correspondientes a los TextView y se crea la logica del botón mas informacion sobre el producto, el cual nos muestra los pasos a seguir del
     * producto por medio de un dialogo, el cual tiene 2 botones, uno para eliminar el producto y otro para dirigirnos a la pantalla editar producto.
     *
     * @param position La posición del elemento en la lista que se está procesando.
     * @param convertView La vista del elemento productos en el que se trabaja.
     * @param parent Los datos usados para conocer las dimensiones y las propiedades del contenedor.
     *
     * @return convertView La vista convertida en viewwHolder para mostrar el producto.
     */
    @SuppressLint("Range")
    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        AdaptadorListaProductos.ViewHolder viewHolder;

        negocioSeleccionado = new Bundle();

        //Reutilizar la vista existente si es posible
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            convertView = inflater.inflate(R.layout.elemento_producto, parent, false);

            //Crear un nuevo ViewHolder
            viewHolder = new AdaptadorListaProductos.ViewHolder();
            viewHolder.nombreProducto = convertView.findViewById(R.id.nombreProducto);
            viewHolder.precioProducto = convertView.findViewById(R.id.precioProducto);
            viewHolder.descripcionProducto = convertView.findViewById(R.id.descripcionProducto);
            viewHolder.imagenProducto = convertView.findViewById(R.id.imagenProducto);
            viewHolder.botonMasInformacionProducto = convertView.findViewById(R.id.botonMasInformacionProducto);

            convertView.setTag(viewHolder);
        } else {
            //Si la vista ya existe, reutilizar el ViewHolder
            viewHolder = (AdaptadorListaProductos.ViewHolder) convertView.getTag();
        }

        //Buscamos el rol y el correo del usuario actual, para en el caso de necesitar gestionar algo dependiendo de su rol, poder hacerlo
        UsuariosSQLite baseDatosUsuarios = new UsuariosSQLite(mContext, "bbddUsuarios", null, 1);
        baseDatos = baseDatosUsuarios.getWritableDatabase();

        String consultaUsuario = "SELECT correo, rol, contrasena FROM usuario LIMIT 1";
        Cursor cursor = baseDatos.rawQuery(consultaUsuario, null);

        if (cursor.moveToFirst()) {
            correoUsuario = cursor.getString(cursor.getColumnIndex("correo"));
            rolUsuario = cursor.getString(cursor.getColumnIndex("rol"));
        }

        //Obtener los datos del producto actual
        Producto productoActual = lista.get(position);

        viewHolder.nombreProducto.setText(productoActual.getNombre());
        DecimalFormat formato = new DecimalFormat("0.00");
        viewHolder.precioProducto.setText(formato.format(productoActual.getPrecio()) + " €");
        viewHolder.descripcionProducto.setText(productoActual.getDescripcion());

        //Obtener la imagen desde Firebase Storage
        String enlaceImagen = productoActual.getFotoCodificada();
        String direcciónReal = enlaceImagen.replace("gs://negocios-de-cocinas.appspot.com/", "");
        StorageReference imageRef = mStorage.child(direcciónReal);

        //Limpiar la imagen anterior mientras se carga la nueva
        viewHolder.imagenProducto.setImageDrawable(null);

        //Cargar la imagen usando Glide
        imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
            Glide.with(mContext)
                    .load(uri)
                    .into(viewHolder.imagenProducto);
        }).addOnFailureListener(exception -> {
            Log.e("STORAGE", "Error al obtener la URL de la imagen", exception);
        });

        View finalConvertView = convertView;

        /*
         * Se da lógica al boton que sirve para mostrar el dialogo con el cual tener los passos a seguir del producto, eliminar lo o dirigirnos a editarlo.
         */
        viewHolder.botonMasInformacionProducto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Dialog dialogoInformacionProductosMenu = new Dialog(mContext);
                dialogoInformacionProductosMenu.setContentView(R.layout.dialogo_mas_informacion_producto);
                dialogoInformacionProductosMenu.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

                TextView textoDescripcion = dialogoInformacionProductosMenu.findViewById(R.id.textoPasosASeguir);
                textoDescripcion.setText(productoActual.getPasosSeguir());
                textoDescripcion.setEnabled(false);

                /*
                 * Se da lógica al boton del dialogo que sirve para dirigirnos a la pantalla de editar producto, llevandonos en un bundle el ID del producto
                 * para poder cargar en esa pantalla el producto y poderlo gestionar.
                 */
                Button botonEditar = dialogoInformacionProductosMenu.findViewById(R.id.botonEditarProducto);
                botonEditar.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String claveProducto = productoActual.getId();
                        DatabaseReference referenciaProductos = FirebaseDatabase.getInstance().getReference("productos");
                        Query busquedaProductos = referenciaProductos.orderByChild("id").equalTo(claveProducto);

                        //Nos aseguramos de que el producto existe para poder enviarlo a editar
                        busquedaProductos.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                if (dataSnapshot.exists()) {
                                    Intent botonIrEditarProductos = new Intent(mContext, EditarProductos.class);
                                    negocioSeleccionado.putString("negocioLoggued", correoUsuario);
                                    negocioSeleccionado.putString("productoSeleccionado", productoActual.getId());
                                    botonIrEditarProductos.putExtras(negocioSeleccionado);
                                    mContext.startActivity(botonIrEditarProductos);
                                } else {
                                    Snackbar.make(v, "El producto '"+productoActual.getNombre() + "' no existe en la base de datos", Snackbar.LENGTH_SHORT).show();
                                }
                            }
                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {
                                Snackbar.make(v, "Error de base de datos", Snackbar.LENGTH_SHORT).show();
                            }
                        });
                    }
                });

                /*
                 * Se da lógica al boton del dialogo que sirve para eliminar el producto seleccionado.
                 */
                Button botonBorrar = dialogoInformacionProductosMenu.findViewById(R.id.botonBorrarProducto);
                botonBorrar.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String claveProducto = productoActual.getId();
                        DatabaseReference referenciaProductos = FirebaseDatabase.getInstance().getReference("productos");
                        Query busquedaProductos = referenciaProductos.orderByChild("id").equalTo(claveProducto);

                        //Nos aseguramos de que el producto existe para poder eliminarlo
                        busquedaProductos.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                if (dataSnapshot.exists()) {
                                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                        snapshot.getRef().removeValue();
                                    }
                                    lista.remove(position);
                                    notifyDataSetChanged();
                                } else {
                                    Snackbar.make(v, "El producto '"+productoActual.getNombre() + "' no existe en la base de datos", Snackbar.LENGTH_SHORT).show();
                                }
                            }
                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {
                                Snackbar.make(v, "Error de base de datos", Snackbar.LENGTH_SHORT).show();
                            }
                        });
                    }
                });

                //En caso de que nuestro rol no sea de administrador, no mostrar las opciones de borrado y edicion
                if (!rolUsuario.equals("Administrador")) {
                    botonBorrar.setVisibility(View.GONE);
                    botonEditar.setVisibility(View.GONE);
                }

                dialogoInformacionProductosMenu.show();
            }
        });
        return convertView;
    }
}
