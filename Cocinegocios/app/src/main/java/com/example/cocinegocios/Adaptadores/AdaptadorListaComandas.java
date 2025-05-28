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
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.cocinegocios.Clases.Comanda;
import com.example.cocinegocios.Clases.ProductoComanda;
import com.example.cocinegocios.Clases.UsuariosSQLite;
import com.example.cocinegocios.GestionarComandas;
import com.example.cocinegocios.R;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.DecimalFormat;
import java.util.ArrayList;

/**
 * Clase con funcion de adaptador para las listas que contengan comandas del negocio.
 * <p>
 * Esta clase o adaptador contiene la lógica necesaria para mostrar las comandas que haya en el negocio donde él usuario esté actualmente. Tambien contiene la logica de las comandas,para
 * cambiar de estado, marcar los productos para notificar cambios en sus procesos y poder pagar las comandas. Por su puesto, para esto es necesario hacer uso del AdaptadorListaProductosComanda.
 * <p>
 * Autor: [Juan Ramón de León Martín]
 * Fecha: [3/12/2024]
 */

public class AdaptadorListaComandas extends ArrayAdapter<Comanda> {

    private ArrayList<Comanda> lista;
    private Context mContext;

    private String negocioActual, correoUsuario, rolUsuario, idComandaActual;

    private ListView listaProductosComanda;
    private ArrayList<ProductoComanda> listaProductosComandaEntrantes = new ArrayList<>();
    private AdaptadorListaProductosComanda adaptadorProductosComanda;

    private boolean listoPagar = false;

    SQLiteDatabase baseDatos;


    /**
     * Constructor del adaptador de la lista de comandas.
     * <p>
     * Este constructor inicializa el adaptador con el contexto, la lista de comandas, y el nombre del negocio en el que estamos actualmente.
     *
     * @param contexto El contexto de la actividad o fragmento donde se utiliza el adaptador.
     * @param lista La lista de objetos de tipo Espacio que se mostrarán en la vista.
     * @param nombreNegocio El nombre del negocio en el que estamos actualmente gestionando las comandas.
     */
    public AdaptadorListaComandas(Context contexto, ArrayList<Comanda> lista, String nombreNegocio) {
        super(contexto, R.layout.elemento_comanda, lista);
        this.lista = lista;
        this.mContext = contexto;
        this.negocioActual = nombreNegocio;

    }

    /**
     * Metodo para inicializar el contenido del listado de comandas.
     * <p>
     * Esta clase inicializa el contenido de cada una de las comandas de la lista de comandas.
     */
    public static class ViewHolder {
        TextView idComanda;
        ImageView estadoComanda;
        TextView contenidoEspacio;
        TextView numeroMesas;
        Button botonInformacionComanda;
    }


    /**
     * Metodo para dar utilidad a las comandas
     * <p>
     * Esta clase coge y asocia los contenidos de cada elemento comanda y los asocia con los del 'elemento_comanda' mediante los ViewHolder. El ViewHolder se crea si no hay uno previamente,
     * en el caso contrario se reutiliza. Luego se pone los datos correspondientes a los TextView y se crea la logica del botón informacion de la comanda, el cual abre un dialogo, donde deberia
     * mostrar el estado de los productos, el subtotal de la comanda y 2 botones, para cerrar o pagar/notificar.
     *
     * @param position La posición del elemento en la lista que se está procesando.
     * @param convertView La vista del elemento comanda en el que se trabaja.
     * @param parent Los datos usados para conocer las dimensiones y las propiedades del contenedor.
     *
     * @return convertView La vista convertida en viewwHolder para mostrar la comanda.
     */
    @SuppressLint("Range")
    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        AdaptadorListaComandas.ViewHolder viewHolder;

        //Reutilizar la vista existente si es posible
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            convertView = inflater.inflate(R.layout.elemento_comanda, parent, false);

            //Crear un nuevo ViewHolder
            viewHolder = new AdaptadorListaComandas.ViewHolder();
            viewHolder.idComanda = convertView.findViewById(R.id.idComanda);
            viewHolder.estadoComanda = convertView.findViewById(R.id.estadoComanda);
            viewHolder.contenidoEspacio = convertView.findViewById(R.id.contenidoEspacio);
            viewHolder.numeroMesas = convertView.findViewById(R.id.numeroMesas);
            viewHolder.botonInformacionComanda = convertView.findViewById(R.id.botonInformacionComanda);

            convertView.setTag(viewHolder);
        } else {
            //Si la vista ya existe, reutilizar el ViewHolder
            viewHolder = (AdaptadorListaComandas.ViewHolder) convertView.getTag();
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

        //Obtener los datos de la comanda actual y settearlos
        Comanda comandaActual = lista.get(position);
        viewHolder.idComanda.setText(comandaActual.getId());

        //Obtener el indicador de cada comanda para poder ponerle un icono que represente su estado
        switch (comandaActual.getIndicador()) {
            //Icono de en producción
            case "enProduccion":
                viewHolder.estadoComanda.setImageResource(R.drawable.boton_en_producion_comanda);
                break;
            //Icono de notificar que hay productos ya para entregar
            case "productosListos":
                viewHolder.estadoComanda.setImageResource(R.drawable.boton_alerta);
                break;
            //Icono de para pagar
            case "listoParaPagar":
                viewHolder.estadoComanda.setImageResource(R.drawable.boton_listo_pagar);
                break;
            //Icono por defecto
            default:
                viewHolder.estadoComanda.setImageResource(android.R.drawable.ic_menu_help);
                break;
        }
        viewHolder.contenidoEspacio.setText(comandaActual.getEspacio());
        viewHolder.numeroMesas.setText(String.valueOf(comandaActual.getNumeroMesa()));
        /*
         * Se da lógica al boton informacion comanda, que muestra toda la informacion necesaria para gestionar la comanda, siendo tanto cocinero como camarero
         */
        viewHolder.botonInformacionComanda.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //Se inicializa el dialogo correspondiente y se da los valores necesarios
                Dialog dialogoComanda = new Dialog(mContext);
                dialogoComanda.setContentView(R.layout.dialogo_comanda);
                dialogoComanda.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                dialogoComanda.setCanceledOnTouchOutside(false);
                dialogoComanda.setCancelable(false);

                idComandaActual = comandaActual.getId();

                obtenerProductosComandaPorIDComanda(idComandaActual);

                listaProductosComanda = dialogoComanda.findViewById(R.id.listaProductos);
                adaptadorProductosComanda = new AdaptadorListaProductosComanda(mContext, listaProductosComandaEntrantes, idComandaActual);
                listaProductosComanda.setAdapter(adaptadorProductosComanda);

                DecimalFormat formato = new DecimalFormat("0.00");
                TextView textoSubtotal = dialogoComanda.findViewById(R.id.subTotalComanda);
                textoSubtotal.setText(formato.format(comandaActual.getSubtotal()) + " €");

                Button botonMarcarComoLeidoComanda = dialogoComanda.findViewById(R.id.botonMarcarComoLeidoComanda);
                Button botonCerrarComanda = dialogoComanda.findViewById(R.id.botonCerrarComanda);

                String indicadorComandaActual = comandaActual.getIndicador();

                if (indicadorComandaActual.equals("listoParaPagar")) {
                    botonMarcarComoLeidoComanda.setText("Pago realizado");
                    listoPagar = true;
                }

                /*
                 * Se da lógica al boton que sirve para notificar los cambios de la comanda actual y sus propios productosComandas, aunque esto depende del rol del usuario
                 * como muestre algunos produtos y sus acciones
                 */
                botonMarcarComoLeidoComanda.setOnClickListener(view -> {

                    //Mediante un boolean generado al principio y tiene su propia logica para saber cuando un pedido esta listo para pagar o no, en el caso de estarlo, gestionamos sus opciones y el cambio de algunas cosas
                    if (listoPagar) {
                        DatabaseReference databaseReferenceComandas = FirebaseDatabase.getInstance().getReference("comandas");
                        Query busquedaComandas = databaseReferenceComandas.orderByChild("id").equalTo(idComandaActual);

                        //Buscamos la comanda actual y la eliminamos
                        busquedaComandas.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                if (dataSnapshot.exists()) {
                                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                        snapshot.getRef().removeValue();
                                    }

                                    DatabaseReference databaseReferenceProductosComanda = FirebaseDatabase.getInstance().getReference("productosComanda");
                                    Query busquedaProductosComanda = databaseReferenceProductosComanda.orderByChild("pedidoAsociado").equalTo(idComandaActual);

                                    busquedaProductosComanda.addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                            if (dataSnapshot.exists()) {
                                                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                                    // Elimina cada productoComanda asociado a la comanda actual
                                                    snapshot.getRef().removeValue();
                                                }
                                            }
                                            if (mContext instanceof GestionarComandas) {
                                                ((GestionarComandas) mContext).actualizarListaComandas();
                                            }

                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError databaseError) {
                                            Snackbar.make(v, "Error al eliminar productos asociados", Snackbar.LENGTH_SHORT).show();
                                        }
                                    });
                                }
                            }
                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {
                                Snackbar.make(v, "Error de base de datos", Snackbar.LENGTH_SHORT).show();
                            }
                        });

                        dialogoComanda.dismiss();
                    } else {
                        //En el caso de no estar la comanda actual en listoParaPagar se recogen los productos cuyo checkbos este marcado para cambiarles su estado
                        DatabaseReference productosComandaRef = FirebaseDatabase.getInstance().getReference("productosComanda");

                        for (int i = 0; i < listaProductosComanda.getChildCount(); i++) {
                            View itemView = listaProductosComanda.getChildAt(i);
                            //Obtén el CheckBox y verifica su estado
                            CheckBox checkboxEstado = itemView.findViewById(R.id.checkboxEstado);
                            if (checkboxEstado.isChecked()) {
                                ProductoComanda productoActual = listaProductosComandaEntrantes.get(i);
                                //Buscamos el producto cuyo checkbox esté marcado
                                DatabaseReference productoRef = productosComandaRef.child(productoActual.getId());

                                //Obtener el estado actual del producto en Firebase, porque dependiendo de en cual esté avanzará a uno u otro.
                                productoRef.child("estado").get().addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        String estadoActual = task.getResult().getValue(String.class);
                                        //Tambien a su vez, cuando se realize un cambio con los productos de la comanda, se debe notificar por parte de los cocineros a los camareros.
                                        if ("enProduccion".equals(estadoActual)) {
                                            //Cambiar de "enProduccion" a "listoParaEntregar"
                                            productoRef.child("estado").setValue("listoParaEntregar").addOnSuccessListener(aVoid -> {
                                                        String estadoComanda = comandaActual.getIndicador();
                                                        if (estadoComanda.equals("enProduccion")) {
                                                            DatabaseReference databaseReferenceCreacion = FirebaseDatabase.getInstance("https://negocios-de-cocinas-default-rtdb.europe-west1.firebasedatabase.app/").getReference();
                                                            DatabaseReference comandaReferencia = databaseReferenceCreacion.child("comandas");
                                                            comandaReferencia.child(idComandaActual).addListenerForSingleValueEvent(new ValueEventListener() {
                                                                @Override
                                                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                                    if (snapshot.exists()) {
                                                                        Comanda comandaExistente = snapshot.getValue(Comanda.class);
                                                                        comandaExistente.setIndicador("productosListos");
                                                                        comandaReferencia.child(idComandaActual).setValue(comandaExistente).addOnSuccessListener(aVoid -> {
                                                                            Snackbar.make(v, "Comanda actualizada", Snackbar.LENGTH_SHORT).show();

                                                                            if (mContext instanceof GestionarComandas) {
                                                                                ((GestionarComandas) mContext).actualizarListaComandas();
                                                                            }
                                                                        }).addOnFailureListener(e -> {
                                                                            Snackbar.make(v, "Error al actualizar el producto", Snackbar.LENGTH_SHORT).show();
                                                                        });
                                                                    }
                                                                }

                                                                @Override
                                                                public void onCancelled(@NonNull DatabaseError error) {
                                                                    Snackbar.make(v, "Error de conexión con la base de datos", Snackbar.LENGTH_SHORT).show();
                                                                }
                                                            });
                                                        }
                                                    })
                                                    .addOnFailureListener(e -> {
                                                        Log.e("EstadoProducto", "Error al actualizar producto " + productoActual.getNombre(), e);
                                                    });

                                            /*
                                             * En el caso de estar listo para entregar serán los camareros los que realizen este cambio de indicacion en la comanda, pero depende de si los productos estan
                                             * todos entregados hará una cosa u otra.
                                             */
                                        } else if ("listoParaEntregar".equals(estadoActual)) {
                                            // Cambiar de "listoParaEntregar" a "entregado"
                                            productoRef.child("estado").setValue("entregado")
                                                    .addOnSuccessListener(aVoid -> {
                                                        Log.d("EstadoProducto", "Producto " + productoActual.getNombre() + " actualizado a entregado.");
                                                        obtenerProductosComandaPorIDComandaCambiarEstadoComanda(idComandaActual);
                                                    })
                                                    .addOnFailureListener(e -> {
                                                        Log.e("EstadoProducto", "Error al actualizar producto " + productoActual.getNombre(), e);
                                                    });
                                        }
                                    } else {
                                        Log.e("EstadoProducto", "Error al obtener estado actual del producto " + productoActual.getNombre(), task.getException());
                                    }
                                });
                            }
                        }

                        //Limpiar la lista de productos
                        listaProductosComandaEntrantes.clear();

                        //Forzar recarga de datos desde Firebase
                        obtenerProductosComandaPorIDComanda(idComandaActual);
                        Snackbar.make(v, "Productos actualizados", Snackbar.LENGTH_SHORT).show();

                        if (mContext instanceof GestionarComandas) {
                            ((GestionarComandas) mContext).actualizarListaComandas();
                        }

                        dialogoComanda.dismiss();
                    }
                });

                //Permitir que el usuario pueda cerrar el dialogo en caso de equivocación
                botonCerrarComanda.setOnClickListener(view -> {
                    dialogoComanda.dismiss();
                });

                dialogoComanda.show();

            }
        });

        return convertView;
    }


    /**
     * Método que filtra de entre todos los productosComanda cuyo pedido asociado coincida con el idComanda de la comanda que estemos actualmente manejando.
     *
     * @param idComanda ID de la comanda que estamos actualmente manejando
     */
    private void obtenerProductosComandaPorIDComanda(String idComanda) {
        DatabaseReference databaseReferenceProductos = FirebaseDatabase.getInstance().getReference().child("productosComanda");

        //Se busca los productosComanda, se filtran y si pasan ese filtro se añaden al listado, que se recarga cada vez que empieze este proceso
        databaseReferenceProductos.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                listaProductosComandaEntrantes.clear();
                for (DataSnapshot productoSnapshot : dataSnapshot.getChildren()) {
                    ProductoComanda productoComanda = productoSnapshot.getValue(ProductoComanda.class);
                    if (productoComanda != null && idComanda.equals(productoComanda.getPedidoAsociado())) {
                        listaProductosComandaEntrantes.add(productoComanda);
                    }
                }
                adaptadorProductosComanda.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }


    /**
     * Método que filtra de entre todos los productosComanda cuyo pedido asociado coincida con el idComanda de la comanda que estemos actualmente manejando. Hasta este punto es igual
     * que el metodo anterior, pero este recoge los estados de los productosComanda de la comanda, para luego verificar si todos los productosComanda estan en el estado de 'entregado',
     * porque si ese es el caso, la comanda está lista para pagar, y cambia el estado de la comanda a listoParaPagar.
     *
     * @param idComanda ID de la comanda que estamos actualmente manejando
     */
    private void obtenerProductosComandaPorIDComandaCambiarEstadoComanda(String idComanda) {
        //Se crea una lista cada vez que se necesite recoger todos los estados de los productos de la comanda
        ArrayList<String> estadoProductosPedido = new ArrayList<>();
        DatabaseReference databaseReferenceProductos = FirebaseDatabase.getInstance().getReference().child("productosComanda");

        //Se busca los productos y filtra por el idComanda para añadir su estado al listado anterior
        databaseReferenceProductos.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot productoSnapshot : dataSnapshot.getChildren()) {
                    ProductoComanda productoComanda = productoSnapshot.getValue(ProductoComanda.class);
                    if (productoComanda != null && idComanda.equals(productoComanda.getPedidoAsociado())) {
                        estadoProductosPedido.add(productoComanda.getEstado());
                    }
                }
                //Actualizar el adaptador
                adaptadorProductosComanda.notifyDataSetChanged();

                //Verificar si todos los productos están entregados para cambiar el indicador de la comanda a listoParaPagar
                if (estadoProductosPedido.stream().allMatch(estado -> "entregado".equals(estado))) {
                    DatabaseReference databaseReferenceCreacion = FirebaseDatabase.getInstance("https://negocios-de-cocinas-default-rtdb.europe-west1.firebasedatabase.app/").getReference();
                    DatabaseReference comandaReferencia = databaseReferenceCreacion.child("comandas");
                    comandaReferencia.child(idComandaActual).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists()) {
                                Comanda comandaExistente = snapshot.getValue(Comanda.class);

                                comandaExistente.setIndicador("listoParaPagar");
                                comandaReferencia.child(idComandaActual).setValue(comandaExistente).addOnSuccessListener(aVoid -> {
                                    if (mContext instanceof GestionarComandas) {
                                        ((GestionarComandas) mContext).actualizarListaComandas();
                                    }
                                }).addOnFailureListener(e -> {
                                });
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                        }
                    });
                } else {
                    //En el caso de que la comanda no tenga todos los productos en entregado, la comanda cambia su indicador a enProduccion
                    DatabaseReference databaseReferenceCreacion = FirebaseDatabase.getInstance("https://negocios-de-cocinas-default-rtdb.europe-west1.firebasedatabase.app/").getReference();
                        DatabaseReference comandaReferencia = databaseReferenceCreacion.child("comandas");
                        comandaReferencia.child(idComandaActual).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                if (snapshot.exists()) {
                                    Comanda comandaExistente = snapshot.getValue(Comanda.class);

                                    comandaExistente.setIndicador("enProduccion");
                                    comandaReferencia.child(idComandaActual).setValue(comandaExistente).addOnSuccessListener(aVoid -> {
                                        if (mContext instanceof GestionarComandas) {
                                            ((GestionarComandas) mContext).actualizarListaComandas();
                                        }
                                    }).addOnFailureListener(e -> {
                                    });
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                            }
                        });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FIREBASE", "Error al obtener los productos: " + error.getMessage());
            }
        });
    }
}
