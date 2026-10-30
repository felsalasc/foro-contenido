package cl.duoc.forocontenido.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "TEMAS", indexes = @Index(name = "IX_TEMAS_CATEGORIA", columnList = "CATEGORIA_ID"))
public class Tema {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;
    @Column(name = "TITULO", nullable = false, length = 200)
    private String titulo;
    @Lob @Column(name = "CONTENIDO", nullable = false)
    private String contenido;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "CATEGORIA_ID", nullable = false, foreignKey = @ForeignKey(name = "FK_TEMAS_CATEGORIA"))
    private Categoria categoria;
    @Column(name = "AUTOR_ID", nullable = false, updatable = false)
    private Long autorId;
    @Column(name = "FECHA_CREACION", nullable = false, updatable = false)
    private OffsetDateTime fechaCreacion;

    protected Tema() { }
    public Tema(String titulo, String contenido, Categoria categoria, Long autorId) {
        actualizar(titulo, contenido, categoria);
        this.autorId = autorId;
        this.fechaCreacion = OffsetDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);
    }
    public void actualizar(String titulo, String contenido, Categoria categoria) {
        this.titulo = titulo.strip();
        this.contenido = contenido;
        this.categoria = categoria;
    }
    public Long getId() { return id; }
    public String getTitulo() { return titulo; }
    public String getContenido() { return contenido; }
    public Categoria getCategoria() { return categoria; }
    public Long getAutorId() { return autorId; }
    public OffsetDateTime getFechaCreacion() { return fechaCreacion; }
}
