package com.example.cocinegocios.Adaptadores;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.example.cocinegocios.Clases.Producto;
import com.example.cocinegocios.Clases.ProductoComanda;
import com.example.cocinegocios.Clases.UsuariosSQLite;
import com.example.cocinegocios.R;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.DecimalFormat;
import java.util.ArrayList;

/**
 * Clase con funcion de adaptador para las listas que contengan productos de las comandas del negocio.
 * <p>
 * Esta clase o adaptador contiene la lógica necesaria para mostrar los productos de las comandas que haya en el negocio donde él usuario esté actualmente.
 * <p>
 * Autor: [Juan Ramón de León Martín]
 * Fecha: [3/12/2024]
 */

public class AdaptadorListaProductosComanda extends ArrayAdapter<ProductoComanda> {

    private ArrayList<ProductoComanda> lista;
    private Context mContext;
    private String idComandaActual;

    private StorageReference mStorage;

    private String correoUsuario, rolUsuario;

    SQLiteDatabase baseDatos;

    /**
     * Constructor del adaptador de la lista de productos de la comanda.
     * <p>
     * Este constructor inicializa el adaptador con el contexto, la lista de productos de la comanda, y el ID de la comanda en el que estamos actualmente.
     *
     * @param contexto El contexto de la actividad o fragmento donde se utiliza el adaptador.
     * @param lista La lista de objetos de tipo ProductoComanda que se mostrarán en la vista.
     * @param idComandaActual El ID de la comanda del negocio en el que estamos actualmente;
     */
    public AdaptadorListaProductosComanda(Context contexto, ArrayList<ProductoComanda> lista, String idComandaActual) {
        super(contexto, R.layout.elemento_productos_comanda_camarero_cocinero, lista);
        this.lista = lista;
        this.mContext = contexto;
        this.idComandaActual = idComandaActual;
        this.mStorage = FirebaseStorage.getInstance().getReference();
    }

    /**
     * Metodo para inicializar el contenido del listado de productos dentro de la comanda actual.
     * <p>
     * Esta clase inicializa el contenido de cada uno de los productos de la lista de productos de la comanda actual.
     */
    public static class ViewHolder {
        TextView nombreProducto;
        TextView cantidadProducto;
        TextView precioProducto;
        CheckBox checkboxEstado;
        ImageView estadoProductoIcono;
        Button botonInformacionProducto;
    }

    /**
     * Metodo para dar utilidad a los productos de las comandas
     * <p>
     * Esta clase coge y asocia los contenidos de cada elemento productoComanda y los asocia con los del 'elemento_productos_comanda_camarero_cocinero' mediante los ViewHolder. El ViewHolder se crea si no hay uno previamente,en el
     * caso contrario se reutiliza. Luego se pone los datos correspondientes a los TextView y se crea la logica para mostrar los productos y su estado actual en la comanda
     *
     * @param position La posición del elemento en la lista que se está procesando.
     * @param convertView La vista del elemento productosComanda en el que se trabaja.
     * @param parent Los datos usados para conocer las dimensiones y las propiedades del contenedor.
     *
     * @return convertView La vista convertida en viewwHolder para mostrar el producto.
     */
    @SuppressLint("Range")
    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        AdaptadorListaProductosComanda.ViewHolder viewHolder;

        //Reutilizar la vista existente si es posible
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            convertView = inflater.inflate(R.layout.elemento_productos_comanda_camarero_cocinero, parent, false);

            //Crear un nuevo ViewHolder
            viewHolder = new AdaptadorListaProductosComanda.ViewHolder();
            viewHolder.nombreProducto = convertView.findViewById(R.id.nombreProducto);
            viewHolder.cantidadProducto = convertView.findViewById(R.id.cantidadProducto);
            viewHolder.precioProducto = convertView.findViewById(R.id.precioProducto);
            viewHolder.estadoProductoIcono = convertView.findViewById(R.id.estadoProductoIcono);
            viewHolder.checkboxEstado = convertView.findViewById(R.id.checkboxEstado);
            viewHolder.botonInformacionProducto = convertView.findViewById(R.id.botonInformacionProducto);

            convertView.setTag(viewHolder);
        } else {
            //Si la vista ya existe, reutilizar el ViewHolder
            viewHolder = (AdaptadorListaProductosComanda.ViewHolder) convertView.getTag();
        }

        //Buscamos el rol del usuario actual, para en el caso de necesitar gestionar algo dependiendo de su rol, poder hacerlo
        UsuariosSQLite baseDatosUsuarios = new UsuariosSQLite(mContext, "bbddUsuarios", null, 1);
        baseDatos = baseDatosUsuarios.getWritableDatabase();

        String consultaUsuario = "SELECT correo, rol, contrasena FROM usuario LIMIT 1";
        Cursor cursor = baseDatos.rawQuery(consultaUsuario, null);

        if (cursor.moveToFirst()) {
            correoUsuario = cursor.getString(cursor.getColumnIndex("correo"));
            rolUsuario = cursor.getString(cursor.getColumnIndex("rol"));
        }

        //Obtener el estado de cada producto de la comanda para poder ponerle un icono que represente su estado
        ProductoComanda productoComandaActual = lista.get(position);

        switch (productoComandaActual.getEstado()) {
            //Icono de producción
            case "enProduccion":
                viewHolder.estadoProductoIcono.setImageResource(R.drawable.boton_en_produccion);
                break;
            //Icono de entrega
            case "listoParaEntregar":
                viewHolder.estadoProductoIcono.setImageResource(android.R.drawable.ic_menu_send);
                break;
            //Icono de entrega
            case "entregado":
                viewHolder.estadoProductoIcono.setImageResource(R.drawable.boton_entregado);
                break;
            //Icono por defecto
            default:
                viewHolder.estadoProductoIcono.setImageResource(android.R.drawable.ic_menu_help);
                break;
        }

        //La visibilidad de los checkbox dependiendo el estado del producto de la comanda actual
        if (rolUsuario.equals("Camarero")) {
            //Los camareros solo ven el checkbox si el estado es "listoParaEntregar"
            if ("listoParaEntregar".equals(productoComandaActual.getEstado())) {
                viewHolder.checkboxEstado.setVisibility(View.VISIBLE);
            } else {
                viewHolder.checkboxEstado.setVisibility(View.GONE);
            }
        } else if (rolUsuario.equals("Cocinero")) {
            //Los cocineros solo ven el checkbox si el estado es "enProduccion"
            if ("enProduccion".equals(productoComandaActual.getEstado())) {
                viewHolder.checkboxEstado.setVisibility(View.VISIBLE);
            } else {
                viewHolder.checkboxEstado.setVisibility(View.GONE);
            }
        } else {
            //Por defecto no mostrar el checkbox
            viewHolder.checkboxEstado.setVisibility(View.GONE);
        }

        viewHolder.nombreProducto.setText(productoComandaActual.getNombre());
        viewHolder.cantidadProducto.setText(String.valueOf(productoComandaActual.getCantidad()));
        DecimalFormat formato = new DecimalFormat("0.00");
        viewHolder.precioProducto.setText(formato.format(productoComandaActual.getPrecioUnidad()) + " €");

        /*
         * Se da lógica al boton que sirve para eliminar el producto, mediante la busqueda por el ID del producto seleccionado, y tras esto se elimina.
         */
        viewHolder.botonInformacionProducto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Dialog dialogoInformacionProductosComandaCreada = new Dialog(mContext);
                dialogoInformacionProductosComandaCreada.setContentView(R.layout.dialogo_informacion_producto);
                dialogoInformacionProductosComandaCreada.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                dialogoInformacionProductosComandaCreada.setCanceledOnTouchOutside(false);
                dialogoInformacionProductosComandaCreada.setCancelable(false);

                //El ID comanda actual sirve para conseguir el id del producto y con ello todos los datos para mostrar todos sobre el producto para los cocineros
                String idProducto = productoComandaActual.getIdProducto();

                DatabaseReference databaseReferenceCreacion = FirebaseDatabase.getInstance("https://negocios-de-cocinas-default-rtdb.europe-west1.firebasedatabase.app/").getReference();
                DatabaseReference productosReferencia = databaseReferenceCreacion.child("productos");

                TextView textoCategoria = dialogoInformacionProductosComandaCreada.findViewById(R.id.categoriaProducto);
                TextView nombreProducto = dialogoInformacionProductosComandaCreada.findViewById(R.id.nombreProducto);
                TextView descripcionProducto = dialogoInformacionProductosComandaCreada.findViewById(R.id.descripcionProducto);
                TextView pasosProducto = dialogoInformacionProductosComandaCreada.findViewById(R.id.pasosProducto);
                ImageView imagenProducto = dialogoInformacionProductosComandaCreada.findViewById(R.id.imagenProducto);
                Button botonCerrar = dialogoInformacionProductosComandaCreada.findViewById(R.id.botonCerrarInformacionProducto);

                productosReferencia.child(idProducto).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            Producto producto = snapshot.getValue(Producto.class);
                            if (producto != null) {
                                textoCategoria.setText(String.valueOf(producto.getCategoria()));
                                nombreProducto.setText(String.valueOf(producto.getNombre()));
                                descripcionProducto.setText(String.valueOf(producto.getDescripcion()));
                                pasosProducto.setText(String.valueOf(producto.getPasosSeguir()));

                                String enlaceImagen = producto.getFotoCodificada();
                                String direcciónReal = enlaceImagen.replace("gs://negocios-de-cocinas.appspot.com/", "");
                                StorageReference imageRef = mStorage.child(direcciónReal);

                                Log.d("Prueba", enlaceImagen);

                                // Limpiar la imagen anterior mientras se carga la nueva
                                imagenProducto.setImageDrawable(null);

                                // Cargar la imagen usando Glide
                                imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                                    Glide.with(mContext)
                                            .load(uri)
                                            .into(imagenProducto);
                                }).addOnFailureListener(exception -> {
                                    Log.e("STORAGE", "Error al obtener la URL de la imagen", exception);
                                });
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Snackbar.make(v, "Error de conexión con la base de datos", Snackbar.LENGTH_SHORT).show();
                    }
                });

                /*
                 * Se da lógica al boton que sirve para salir de la informacion del producto de la comanda
                 */
                botonCerrar.setOnClickListener(view -> {
                    dialogoInformacionProductosComandaCreada.dismiss();
                });

                dialogoInformacionProductosComandaCreada.show();
            }
        });
        return convertView;
    }
}
