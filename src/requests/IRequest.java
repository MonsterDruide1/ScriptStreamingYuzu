package requests;

import java.util.concurrent.ExecutionException;

public interface IRequest<T> {
	
	public void onComplete(T result) throws InterruptedException, ExecutionException;

}
