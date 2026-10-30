package cl.duoc.forocontenido.model;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "COMENTARIOS", indexes = @Index(name = "IX_COMENTARIOS_TEMA", columnList = "TEMA_ID"))
public class Comentario {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;
    @Lob @Column(name = "CONTENIDO", nullable = false)
    private String contenido;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "TEMA_ID", nullable = false, updatable = false, foreignKey = @ForeignKey(name = "FK_COMENTARIOS_TEMA"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Tema tema;
    @Column(name = "AUTOR_ID", nullable = false, updatable = false)
    private Long autorId;
    @Column(name = "FECHA_CREACION", nullable = false, updatable = false)
    private OffsetDateTime fechaCreacion;

    protected Comentario() { }
    public Comentario(String contenido, Tema tema, Long autorId) {
        this.contenido = contenido;
        this.tema = tema;
        this.autorId = autorId;
        this.fechaCreacion = OffsetDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);
    }
    public void actualizar(String contenido) { this.contenido = contenido; }
    public Long getId() { return id; }
    public String getContenido() { return contenido; }
    public Tema getTema() { return tema; }
    public Long getAutorId() { return autorId; }
    public OffsetDateTime getFechaCreacion() { return fechaCreacion; }
}
