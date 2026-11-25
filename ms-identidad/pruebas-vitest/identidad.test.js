import { describe, it, expect } from 'vitest';

const url = 'http://localhost:8081';
const correo = process.env.CLIENTE_EMAIL;
const clave = process.env.CLIENTE_PASSWORD;

async function login(correo, clave) {
    return fetch(`${url}/api/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ correo, clave })
    });
}

describe('Identidad', () => {
    it('rechaza un login incorrecto', async () => {
        const respuesta = await login('no-existe@example.invalid', 'ClaveIncorrecta123');
        expect(respuesta.status).toBe(401);
    });

    it('no lista usuarios sin token', async () => {
        const respuesta = await fetch(`${url}/api/usuarios`);
        expect(respuesta.status).toBe(401);
    });

    it.skipIf(!correo || !clave)('permite iniciar sesión y consultar el perfil', async () => {
        const respuesta = await login(correo, clave);
        expect(respuesta.status).toBe(200);
        const datos = await respuesta.json();
        expect(datos.token).toBeTruthy();
        const perfil = await fetch(`${url}/api/usuarios/me`, {
            headers: { Authorization: `Bearer ${datos.token}` }
        });
        expect(perfil.status).toBe(200);
        expect((await perfil.json()).correo).toBe(correo.toLowerCase());
    });

    it.skipIf(!correo || !clave)('no permite al cliente listar usuarios', async () => {
        const respuesta = await login(correo, clave);
        expect(respuesta.status).toBe(200);
        const datos = await respuesta.json();
        expect(datos.usuario.rol).toBe('cliente');
        const usuarios = await fetch(`${url}/api/usuarios`, {
            headers: { Authorization: `Bearer ${datos.token}` }
        });
        expect(usuarios.status).toBe(403);
    });
});
