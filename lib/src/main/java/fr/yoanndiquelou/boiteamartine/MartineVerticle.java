package fr.yoanndiquelou.boiteamartine;

import java.awt.FontFormatException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Launcher;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;

public class MartineVerticle extends AbstractVerticle {

	private String root = "webroot";

	private HttpServer server;
	private Drawer mDrawer;

	// Called when verticle is deployed
	public void start(Promise<Void> startPromise) {
		server = vertx.createHttpServer();
		mDrawer = new Drawer();
		Router router = Router.router(vertx);
		router.route().handler(BodyHandler.create());
		router.route("/images").handler(ctx -> {
			JsonObject object = new JsonObject();
			List<String> fileList;
			try {
				fileList = Files.list(Path.of(root)).filter(p -> {
					String mimeType;
					try {
						mimeType = Files.probeContentType(p);
						return mimeType.startsWith("image/");
					} catch (IOException e) {
						return false;
					}
				}).map(p -> p.getFileName().toString()).collect(Collectors.toList());
			} catch (

			IOException e) {
				fileList = new ArrayList<>();
			}
			object.put("images", new JsonArray(fileList));
			HttpServerResponse response = ctx.response();
			response.putHeader("content-type", "application/json");
			response.end(object.encodePrettily());
		});
		router.route(HttpMethod.POST, "/generate").handler(ctx -> {
			JsonObject content = ctx.body().asJsonObject();
			HttpServerResponse response = ctx.response();
			try {
				BufferedImage img = mDrawer.processImage(Path.of(root + "/" + content.getString("image")),
						content.getString("name"), content.getString("description"));
				response.putHeader("content-type", "image/jpeg");
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				ImageIO.write(img, "png", baos);
				byte[] imageBytes = baos.toByteArray();
				response.putHeader("content-length", "" + imageBytes.length);
				response.write(Buffer.buffer().appendBytes(imageBytes));
				response.end();
			} catch (IOException | FontFormatException e) {
				e.printStackTrace();
				response.putHeader("content-type", "application/json");
				response.end(new JsonObject().put("error", e.getLocalizedMessage()).encodePrettily());
			}
		});
		router.route("/*").handler(StaticHandler.create(root));
		router.route().handler(ctx -> {

			// This handler will be called for every request
			HttpServerResponse response = ctx.response();
			response.putHeader("content-type", "text/plain");

			// Write to the response and end it
			response.end("Hello World from Vert.x-Web!");
		});
//		server.requestHandler(req -> {
//			req.response().putHeader("content-type", "text/plain").end("Hello from Vert.x!");
//		});

		server.requestHandler(router).listen(8080);
	}

	// Optional - called when verticle is undeployed
	public void stop() {
	}

	public static void main(String[] args) {
		Launcher launcher = new Launcher();
		launcher.execute("run", MartineVerticle.class.getCanonicalName());
	}
}
