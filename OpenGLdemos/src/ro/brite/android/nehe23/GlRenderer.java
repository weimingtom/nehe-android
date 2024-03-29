package ro.brite.android.nehe23;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import ro.brite.android.opengl.R;
import ro.brite.android.opengl.common.GlMatrix;
import ro.brite.android.opengl.common.GlVertex;
import ro.brite.android.opengl.common.Utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLU;
import android.opengl.GLUtils;
import android.opengl.GLSurfaceView.Renderer;


public class GlRenderer implements Renderer {

	private Context context;
	
	public GlRenderer(Context context) {
		this.context = context;
	}
	
	private final static float lightAmb[]= { 0.5f, 0.5f, 0.5f, 1.0f };
	private final static float lightDif[]= { 1.0f, 1.0f, 1.0f, 1.0f };
	private final static float lightPos[]= { 0.0f, 0.0f, 2.0f, 1.0f };
	
	private final static FloatBuffer lightAmbBfr;
	private final static FloatBuffer lightDifBfr;
	private final static FloatBuffer lightPosBfr;
	
	private IntBuffer texturesBuffer;
	
	private static GlCube cube;
	private static GlCylinder cylinder;
	private static GlDisk disk;
	private static GlSphere sphere;
	private static GlCylinder cone;
	private static GlDisk partialDisk;
	
	private static GlPlane background;

	static final SceneState sceneState;
	private long lastMillis;
	
	static {
		lightAmbBfr = FloatBuffer.wrap(lightAmb);
		lightDifBfr = FloatBuffer.wrap(lightDif);
		lightPosBfr = FloatBuffer.wrap(lightPos);

		cube = new GlCube(1.0f);
		cylinder = new GlCylinder(1.0f, 1.0f, 3.0f, 16, 4);
		disk = new GlDisk(0.5f, 1.5f, 16, 4);
		sphere = new GlSphere(1.3f, 16, 8);
		cone = new GlCylinder(1.0f, 0.0f, 3.0f, 16, 4);
		partialDisk = new GlDisk(0.5f, 1.5f, 16, 4, (float) (Math.PI / 4), (float) (7 * Math.PI / 4));
		
		background = new GlPlane(16, 12, true, true);
		
		sceneState = new SceneState();
	}

	private void LoadTextures(GL10 gl) {
		
		gl.glEnable(GL10.GL_TEXTURE_2D);
		int[] textureIDs = new int[] { R.drawable.nehe_texture_background, R.drawable.nehe_texture_background_sphere_map };
		
		// create textures
		texturesBuffer = IntBuffer.allocate(3 * textureIDs.length);
		gl.glGenTextures(3, texturesBuffer);
		
		for (int i = 0; i < textureIDs.length; i++) {
			// load bitmap
			Bitmap texture = Utils.getTextureFromBitmapResource(context, textureIDs[i]);
			
			// setup texture 0
			gl.glBindTexture(GL10.GL_TEXTURE_2D, texturesBuffer.get(3 * i));
			gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_NEAREST);
			gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
			gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
			gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);
			GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, texture, 0);
			
			// setup texture 1
			gl.glBindTexture(GL10.GL_TEXTURE_2D, texturesBuffer.get(3 * i + 1));
			gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
			gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
			gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
			gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);
			GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, texture, 0);
			
			// setup texture 2
			gl.glBindTexture(GL10.GL_TEXTURE_2D, texturesBuffer.get(3 * i + 2));
			gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
			gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR_MIPMAP_NEAREST);
			gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
			gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);
			Utils.generateMipmapsForBoundTexture(texture);
	
			// free bitmap
			texture.recycle();
		}
	}
	
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		gl.glShadeModel(GL10.GL_SMOOTH);
		gl.glClearColor(0, 0, 0, 0);

		gl.glClearDepthf(1.0f);
		gl.glEnable(GL10.GL_DEPTH_TEST);
		gl.glDepthFunc(GL10.GL_LEQUAL);
		
		gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_NICEST);
		
		gl.glCullFace(GL10.GL_BACK);
		
		// lighting
		gl.glEnable(GL10.GL_LIGHT0);
		gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_AMBIENT, lightAmbBfr);
		gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_DIFFUSE, lightDifBfr);
		gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_POSITION, lightPosBfr);
	}
	
	public void onDrawFrame(GL10 gl) {
		synchronized (sceneState) {
			// freeze scene state variables
			sceneState.takeDataSnapshot();
		}
		
		gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
		gl.glMatrixMode(GL10.GL_MODELVIEW);
		gl.glLoadIdentity();

		// set object color
		gl.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
		
		// update lighting
		if (sceneState.lighting) {
			gl.glEnable(GL10.GL_LIGHTING);
		} else {
			gl.glDisable(GL10.GL_LIGHTING);
		}
		
		// draw background
		gl.glPushMatrix();
		gl.glTranslatef(0, 0, -10);
		gl.glBindTexture(GL10.GL_TEXTURE_2D, texturesBuffer.get(sceneState.filter));
		background.draw(gl);
		gl.glPopMatrix();
		
		GlVertex vEye;
		GlMatrix mRot;
		synchronized (sceneState) {
			// position object
			gl.glTranslatef(0, 0, -6);
			sceneState.rotateModel(gl);
	
			// compute reflection variables
			
			vEye = new GlVertex(0, 0, 1);						// eye-vector in world space (eye is in the scene at Znear = 1.0f)
			GlMatrix mInv = sceneState.getInverseRotation();	// build the current inverse matrix
			mInv.translate(0, 0, 6);
			mInv.transform(vEye);								// transform the eye-vector in model space
	
			mRot = sceneState.getRotation();					// rotation matrix, used for transforming the reflection vector
		}
		
		// identify object to draw
		GlObject object = null;
		boolean doubleSided = false;
		switch (sceneState.objectIdx) {
		case 0:
			object = cube;
			doubleSided = false;
			break;
		case 1:
			object = cylinder;
			doubleSided = true;
			break;
		case 2:
			object = disk;
			doubleSided = true;
			break;
		case 3:
			object = sphere;
			doubleSided = false;
			break;
		case 4:
			object = cone;
			doubleSided = true;
			break;
		case 5:
			object = partialDisk;
			doubleSided = true;
			break;
		}

		// adjust rendering parameters
		if (doubleSided) {
			gl.glDisable(GL10.GL_CULL_FACE);
			gl.glLightModelx(GL10.GL_LIGHT_MODEL_TWO_SIDE, sceneState.lighting ? GL10.GL_TRUE : GL10.GL_FALSE);
		} else {
			gl.glEnable(GL10.GL_CULL_FACE);
			gl.glLightModelx(GL10.GL_LIGHT_MODEL_TWO_SIDE, GL10.GL_FALSE);
		}
		
		// draw object
		gl.glBindTexture(GL10.GL_TEXTURE_2D, texturesBuffer.get(3 + sceneState.filter));
		object.calculateReflectionTexCoords(vEye, mRot);
		object.draw(gl);
		
		// get current millis
		long currentMillis = System.currentTimeMillis();
		
		// update rotations
		if (lastMillis != 0) {
			long delta = currentMillis - lastMillis;
			synchronized (sceneState) {
				sceneState.dx += sceneState.dxSpeed * delta;
				sceneState.dy += sceneState.dySpeed * delta;
				sceneState.dampenSpeed(delta);
			}
		}
		
		// update millis
		lastMillis = currentMillis;
	}

	public void onSurfaceChanged(GL10 gl, int width, int height) {
		// reload textures
		LoadTextures(gl);
		// avoid division by zero
		if (height == 0) height = 1;
		// draw on the entire screen
		gl.glViewport(0, 0, width, height);
		// setup projection matrix
		gl.glMatrixMode(GL10.GL_PROJECTION);
		gl.glLoadIdentity();
		GLU.gluPerspective(gl, 45.0f, (float)width / (float)height, 1.0f, 100.0f);
	}
	
}
