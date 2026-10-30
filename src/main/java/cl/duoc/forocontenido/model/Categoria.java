package cl.duoc.forocontenido.model;

import jakarta.persistence.*;
import java.util.Locale;

@Entity
@Table(name = "CATEGORIAS", uniqueConstraints = @UniqueConstraint(name = "UK_CATEGORIAS_CLAVE", columnNames = "CLAVE_NOMBRE"))
public class Categoria {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;
    @Column(name = "NOMBRE", nullable = false, length = 100)
    private String nombre;
    // El mayusculado puede expandir caracteres: ß -> SS.
    @Column(name = "CLAVE_NOMBRE", nullable = false, length = 300)
    private String claveNombre;
    @Column(name = "DESCRIPCION", length = 500)
    private String descripcion;

    protected Categoria() { }
    public Categoria(String nombre, String descripcion) { actualizar(nombre, descripcion); }

    public void actualizar(String nombre, String descripcion) {
        this.nombre = nombre.strip();
        this.claveNombre = clave(this.nombre);
        this.descripcion = descripcion == null || descripcion.isBlank() ? null : descripcion.strip();
    }
    public static String clave(String nombre) { return nombre.strip().toUpperCase(Locale.ROOT); }
    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
}
